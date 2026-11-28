<template>
  <div class="login">
    <!-- 顶部品牌 -->
    <div class="login-brand">
      <img src="@/assets/logo/logo.png" alt="logo" class="brand-logo" />
      <span class="brand-name">{{ title }}</span>
    </div>
    <!-- 左侧业务标语 -->
    <div class="login-slogan">
      <h1>保税维修工作台</h1>
      <p>单证备案 · 出入存联动 · 海关申报</p>
    </div>
    <!-- 右侧登录卡片 -->
    <div class="login-panel-wrap">
      <div class="panel-version">保税维修版V1.0.0</div>
      <div class="login-form">
        <h3 class="panel-title">登录</h3>
        <el-form ref="loginRef" :model="loginForm" :rules="loginRules">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              size="large"
              auto-complete="off"
              placeholder="账号"
            >
              <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <el-input
              v-model="loginForm.code"
              size="large"
              auto-complete="off"
              placeholder="验证码"
              style="width: 63%"
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img"/>
            </div>
          </el-form-item>
          <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
          <el-form-item style="width:100%;">
            <el-button
              :loading="loading"
              size="large"
              type="primary"
              style="width:100%;"
              @click.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
            </el-button>
            <div style="float: right;" v-if="register">
              <router-link class="link-type" :to="'/register'">立即注册</router-link>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <!-- 底部版权 -->
    <div class="login-footer">Copyright © 2026 {{ title }} · 保税维修工作台</div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'

const title = import.meta.env.VITE_APP_TITLE
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        // 重新获取验证码
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
/* 全屏业务背景（电子制造维修主题）+ 暗化渐变，保证白色标语与右侧卡片可读 */
.login {
  position: relative;
  height: 100%;
  overflow: hidden;
  background-image:
    linear-gradient(105deg, rgba(10, 20, 30, 0.55) 0%, rgba(10, 20, 30, 0.25) 45%, rgba(255, 255, 255, 0.28) 72%),
    url("../assets/images/login-bg.jpg");
  background-size: cover;
  background-position: center;
}

/* 顶部品牌 */
.login-brand {
  position: absolute;
  top: 24px;
  left: 36px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 2;
  .brand-logo {
    height: 40px;
    width: auto;
  }
  .brand-name {
    color: #ffffff;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 2px;
  }
}

/* 左侧大标语 */
.login-slogan {
  position: absolute;
  left: 8%;
  top: 50%;
  transform: translateY(-58%);
  color: #ffffff;
  z-index: 2;
  h1 {
    margin: 0 0 18px 0;
    font-size: 60px;
    font-weight: 700;
    line-height: 1.25;
    letter-spacing: 6px;
    text-shadow: 0 4px 24px rgba(0, 0, 0, 0.45);
  }
  p {
    margin: 0;
    font-size: 18px;
    letter-spacing: 4px;
    opacity: 0.85;
  }
}

/* 右侧登录卡片 */
.login-panel-wrap {
  position: absolute;
  right: 9%;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 420px;
  .panel-version {
    margin-bottom: 10px;
    text-align: right;
    color: rgba(255, 255, 255, 0.8);
    font-size: 12px;
    letter-spacing: 1px;
  }
}

.login-form {
  border-radius: 8px;
  background: #ffffff;
  padding: 32px 36px 20px 36px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.35);
  .panel-title {
    margin: 0 0 24px 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-color-primary);
    text-align: center;
  }
  .el-input {
    height: 40px;
    input {
      height: 40px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}
.login-code {
  width: 33%;
  height: 40px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.login-code-img {
  height: 40px;
  padding-left: 12px;
}

/* 底部版权 */
.login-footer {
  position: absolute;
  bottom: 16px;
  left: 36px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  letter-spacing: 1px;
  z-index: 2;
}

/* 窄屏：隐藏左侧标语，卡片居中 */
@media (max-width: 992px) {
  .login-slogan {
    display: none;
  }
  .login-panel-wrap {
    right: 50%;
    transform: translate(50%, -50%);
  }
}

html.dark .login {
  background-image:
    linear-gradient(105deg, rgba(0, 0, 0, 0.65) 0%, rgba(0, 0, 0, 0.45) 45%, rgba(0, 0, 0, 0.55) 72%),
    url("../assets/images/login-bg.jpg");
  .login-form {
    background: var(--el-bg-color-overlay) !important;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
  }
}
</style>
