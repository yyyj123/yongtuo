import { mount } from '@vue/test-utils'
import { ElButton, ElInput } from 'element-plus'
import { describe, expect, it } from 'vitest'
import LoginView from './LoginView.vue'

const mountLoginView = () => mount(LoginView, {
  global: {
    components: { ElButton, ElInput }
  }
})

describe('LoginView', () => {
  it('renders username, password, and submit controls for administrator sign-in', () => {
    const wrapper = mountLoginView()

    expect(wrapper.find('input[name="username"]').isVisible()).toBe(true)
    expect(wrapper.find('input[name="password"][type="password"]').isVisible()).toBe(true)
    expect(wrapper.find('button[type="submit"]').isVisible()).toBe(true)
  })

  it('does not render the inaccessible password-visibility icon container', () => {
    const wrapper = mountLoginView()

    const passwordInput = wrapper.get('input[name="password"]')
    const passwordContainer = passwordInput.element.closest('.el-input')

    expect(passwordContainer?.classList.contains('el-input--suffix')).toBe(false)
  })

  it('keeps the submit control keyboard reachable with a visible focus target', () => {
    const wrapper = mountLoginView()
    const submit = wrapper.get('button[type="submit"]')

    expect(submit.attributes('tabindex')).not.toBe('-1')
    expect(submit.classes()).toContain('login-submit')
  })
})
