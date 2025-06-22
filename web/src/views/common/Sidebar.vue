<template>
  <div class="sidebar-haslogo">
    <div class="header-logo">
      <a href="/dashboard" id="logo">
        <img src="/static/img/logo.png" alt="" />
      </a>
    </div>
    <a-menu
      mode="inline"
      style="border-right-color: transparent"
      :open-keys="openKeys"
      @openChange="onOpenChange"
      :selectedKeys="[onRoutes]"
    >
      <template v-for="item in filteredSidebar">
        <a-menu-item v-if="!item.children" :key="item.path">
          <router-link :to="{ path: item.path }">
            <a-icon :type="item.meta.icon"></a-icon>
            <span>{{ item.meta.title }}</span>
          </router-link>
        </a-menu-item>
        <sub-menu v-else :key="item.path" :menu-info="item" />
      </template>
    </a-menu>
  </div>
</template>

<script>
import { Menu } from "ant-design-vue";
import router from "@/router/index.js";
import mixin from "@/mixins/index";
import { mapGetters } from 'vuex';

const SubMenu = {
  template: `
      <a-sub-menu :key="menuInfo.path" v-bind="$props" v-on="$listeners">
        <span slot="title">
          <a-icon :type="menuInfo.meta.icon" />
          <span>{{ menuInfo.meta.title }}</span>
        </span>
        <template v-for="item in filterAdminRoutes(menuInfo.children)">
          <a-menu-item v-if="!item.children" :key="item.path">
            <router-link :to="{path: item.path}">
              <span>{{ item.meta.title }}</span>
            </router-link>
          </a-menu-item>
          <sub-menu v-else :key="item.path" :menu-info="item" />
        </template>
      </a-sub-menu>
    `,
  name: "SubMenu",
  isSubMenu: true,
  props: {
    ...Menu.SubMenu.props,
    menuInfo: {
      type: Object,
      default: () => ({}),
    },
  },
  computed: {
    ...mapGetters(['USER_INFO']),
    isAdmin() {
      return this.USER_INFO && this.USER_INFO.isAdmin == '1';
    }
  },
  methods: {
    filterAdminRoutes(routes) {
      if (!routes) return [];
      return routes.filter(route => {
        return !route.meta.isAdmin || (route.meta.isAdmin && this.isAdmin);
      });
    }
  }
};

export default {
  mixins: [mixin],
  components: {
    "sub-menu": SubMenu,
  },
  data() {
    return {
      // 侧边栏
      sidebar: router.options.routes[1].children,
      rootSubmenuKeys: ["/setting", "/config"],
      openKeys: ["/config"],
    };
  },
  computed: {
    ...mapGetters(['USER_INFO']),
    isAdmin() {
      return this.USER_INFO && this.USER_INFO.isAdmin == '1';
    },
    onRoutes() {
      console.log(this.$route.path);
      // 切换页面时摧毁所有弹框
      this.$message.destroy()
      return this.$route.path;
    },
    filteredSidebar() {
      return this.sidebar.filter(route => {
        // 判断管理员页面
        // 判断是否为不显示的子页面
        return (!route.meta.isAdmin || (route.meta.isAdmin && this.isAdmin)) && !route.meta.hideInMenu;
      });
    }
  },
  methods: {
    onOpenChange(openKeys) {
      const latestOpenKey = openKeys.find((key) => this.openKeys.indexOf(key) === -1);
      if (this.rootSubmenuKeys.indexOf(latestOpenKey) === -1) {
        this.openKeys = openKeys;
      } else {
        this.openKeys = latestOpenKey ? [latestOpenKey] : [];
      }
      console.log(this.openKeys);
    },
  },
};
</script>

<style scoped lang="scss">
.sidebar-haslogo {
  height: 100%;
  overflow: hidden auto;
}
.header-logo {
  padding: 8px 16px;
}
#logo {
  overflow: hidden;
  height: 48px;
  line-height: 48px;
  text-decoration: none;
  white-space: nowrap;
  img {
    height: 32px;
    margin: 0 10px;
  }
}
</style>