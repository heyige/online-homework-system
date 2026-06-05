import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarOpened = ref(true)
  const device = ref('desktop')
  const theme = ref('light')
  
  const toggleSidebar = () => {
    sidebarOpened.value = !sidebarOpened.value
  }
  
  const closeSidebar = () => {
    sidebarOpened.value = false
  }
  
  const setDevice = (deviceType) => {
    device.value = deviceType
  }
  
  const setTheme = (themeType) => {
    theme.value = themeType
  }
  
  return {
    sidebarOpened,
    device,
    theme,
    toggleSidebar,
    closeSidebar,
    setDevice,
    setTheme
  }
})
