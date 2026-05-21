<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<script setup>

</script>

<style>
/* 这里写全局样式，不需要加 scoped */

/* 清除浏览器默认的边距，防止页面周围有一圈白边 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
}

#app {
  /* 确保父容器占满全屏，这样 HomeView 的渐变背景才能铺满 */
  height: 100%;
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>