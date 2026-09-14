package com.yongtuo.site.media;

import com.yongtuo.site.common.BusinessException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminMediaService {
 private final JdbcTemplate jdbc; private final ObjectStorageUploadService storage;private final MediaFileMapper media;
 public AdminMediaService(JdbcTemplate jdbc,ObjectStorageUploadService storage,MediaFileMapper media){this.jdbc=jdbc;this.storage=storage;this.media=media;}
 @Transactional public MediaFile upload(MultipartFile file){
  String name=file.getOriginalFilename();
  if(name==null||name.length()>255||name.contains("/")||name.contains("\\")||name.chars().anyMatch(c->c<32)||file.isEmpty()||file.getSize()>20L*1024*1024)throw invalid();
  String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
  String mime=Map.of("jpg","image/jpeg","jpeg","image/jpeg","png","image/png","webp","image/webp","pdf","application/pdf").get(ext);
  if(mime==null||!mime.equals(file.getContentType()))throw invalid();
  byte[] bytes;try{bytes=file.getBytes();}catch(Exception e){throw invalid();}
  if(!signature(ext,bytes))throw invalid();
  String key="managed/"+UUID.randomUUID()+"."+ext;
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{storage.delete(key);}catch(RuntimeException ignored){}}});
  StoredObject stored;
  try{stored=storage.upload(key,name,mime,bytes);}catch(BusinessException e){throw e;}catch(RuntimeException e){throw new BusinessException(37002,"文件上传失败，请稍后重试。",HttpStatus.SERVICE_UNAVAILABLE);}
  try{if(stored==null||!key.equals(stored.storageKey())||stored.publicUrl()==null||stored.publicUrl().length()>1024||!Set.of("https","http").contains(URI.create(stored.publicUrl()).getScheme())||URI.create(stored.publicUrl()).getHost()==null)throw new IllegalArgumentException();}catch(RuntimeException e){throw invalid();}
  MediaFile row=new MediaFile();row.setStorageKey(key);row.setOriginalName(name);row.setPublicUrl(stored.publicUrl());row.setFileType(ext.equals("pdf")?"PDF":"IMAGE");row.setMimeType(mime);row.setFileSize((long)bytes.length);row.setStatus("ACTIVE");
  try{row.setChecksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));}catch(Exception e){throw new IllegalStateException();}
  media.insert(row);return row;
 }
 private static boolean signature(String ext,byte[] b){
  if(b.length<4)return false;
  return switch(ext){case "jpg","jpeg"->(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;
   case "png"->b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
   case "webp"->b.length>=12&&new String(b,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(b,8,4,StandardCharsets.US_ASCII).equals("WEBP");
   case "pdf"->b.length>=5&&new String(b,0,5,StandardCharsets.US_ASCII).equals("%PDF-");default->false;};
 }
 private void product(long id,boolean lock){if(jdbc.queryForList("SELECT id FROM product WHERE id=? AND deleted_at IS NULL"+(lock?" FOR UPDATE":""),Long.class,id).isEmpty())throw new BusinessException(37003,"产品不存在或已删除。",HttpStatus.NOT_FOUND);}
 private MediaFile validMedia(Long id,boolean image){MediaFile row=id==null?null:media.selectById(id);if(row==null||row.getDeletedAt()!=null||!"ACTIVE".equals(row.getStatus())||(image&&!row.getMimeType().startsWith("image/")))throw invalid();return row;}
 public List<ImageRow> images(long id){product(id,false);return jdbc.query("SELECT id,media_id,image_url,alt_zh,alt_en,sort_order,is_cover FROM product_image WHERE product_id=? ORDER BY sort_order,id",new DataClassRowMapper<>(ImageRow.class),id);}
 public List<AttachmentRow> attachments(long id){product(id,false);return jdbc.query("SELECT id,media_id,file_name,file_url,display_name_zh,display_name_en,is_public,allow_download,sort_order FROM product_attachment WHERE product_id=? ORDER BY sort_order,id",new DataClassRowMapper<>(AttachmentRow.class),id);}
 @Transactional public List<ImageRow> images(long id,List<ImageInput> rows){
  product(id,true);if(rows.size()>100||rows.stream().filter(ImageInput::isCover).count()>1||rows.stream().map(ImageInput::mediaId).distinct().count()!=rows.size())throw invalid();
  var files=rows.stream().map(r->validMedia(r.mediaId(),true)).toList();jdbc.update("DELETE FROM product_image WHERE product_id=?",id);String cover=null;
  for(int i=0;i<rows.size();i++){var r=rows.get(i);var m=files.get(i);jdbc.update("INSERT INTO product_image(product_id,media_id,image_url,alt_zh,alt_en,sort_order,is_cover) VALUES (?,?,?,?,?,?,?)",id,m.getId(),m.getPublicUrl(),r.altZh(),r.altEn(),r.sortOrder(),r.isCover());if(r.isCover())cover=m.getPublicUrl();}
  jdbc.update("UPDATE product SET cover_image=? WHERE id=?",cover,id);return images(id);
 }
 @Transactional public List<AttachmentRow> attachments(long id,List<AttachmentInput> rows){
  product(id,true);if(rows.size()>100)throw invalid();var files=rows.stream().map(r->validMedia(r.mediaId(),false)).toList();jdbc.update("DELETE FROM product_attachment WHERE product_id=?",id);
  for(int i=0;i<rows.size();i++){var r=rows.get(i);var m=files.get(i);jdbc.update("INSERT INTO product_attachment(product_id,media_id,file_name,file_url,file_type,display_name_zh,display_name_en,is_public,allow_download,sort_order) VALUES (?,?,?,?,?,?,?,?,?,?)",id,m.getId(),m.getOriginalName(),m.getPublicUrl(),m.getMimeType(),r.displayNameZh(),r.displayNameEn(),r.isPublic(),r.allowDownload(),r.sortOrder());}return attachments(id);
 }
 private static BusinessException invalid(){return new BusinessException(37001,"文件或媒体信息无效。请使用不超过 20 MB 的 JPG、PNG、WebP 或 PDF，并检查媒体引用。",HttpStatus.BAD_REQUEST);}
 public record ImageInput(@jakarta.validation.constraints.NotNull Long mediaId,@jakarta.validation.constraints.Size(max=200) String altZh,@jakarta.validation.constraints.Size(max=200) String altEn,@jakarta.validation.constraints.Min(0) int sortOrder,boolean isCover){}
 public record AttachmentInput(@jakarta.validation.constraints.NotNull Long mediaId,@jakarta.validation.constraints.Size(max=200) String displayNameZh,@jakarta.validation.constraints.Size(max=200) String displayNameEn,boolean isPublic,boolean allowDownload,@jakarta.validation.constraints.Min(0) int sortOrder){}
 public record ImageRow(long id,Long mediaId,String imageUrl,String altZh,String altEn,int sortOrder,boolean isCover){}
 public record AttachmentRow(long id,Long mediaId,String fileName,String fileUrl,String displayNameZh,String displayNameEn,boolean isPublic,boolean allowDownload,int sortOrder){}
}
