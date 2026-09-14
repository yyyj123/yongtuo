package com.yongtuo.site.auth;

record AdminAccount(long id, String username, String passwordHash, int tokenVersion) {
}
