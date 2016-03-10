# ACplug
ACplug Wi-Fi智能插座 App


WiFi智能插座App, 包含 MQTT,Socket,Http,控制手机WiFi连接

  app在远程模式下通过MQTT远程控制设备
  app在本地模式下，不断的发送UDP广播发现设备，发现设备之后通过Socket连接，使用本地控制
  手机注册和设备绑定采用的是 okhttp 包含 http的get、post、和put请求方式
  切换WiFi和通过app使手机连接到指定WiFi上去（当手机连接上设备的AP之后，通过手机系统扫描周围的WiFi信息，选择自己的家庭WiFi,输入密码，把WiFi的SSID和密码 发送给设设备，如果设备返回true,手机开始连接SSID这个WiFi,连接成功之后返回到设备列表界面）
