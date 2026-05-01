import SockJS from 'sockjs-client'
import Stomp from 'stompjs'

class WebSocketService {
  constructor() {
    this.stompClient = null
    this.connected = false
    this.subscriptions = new Map()
    this.messageHandlers = new Map()
  }

  connect() {
    return new Promise((resolve, reject) => {
      if (this.connected && this.stompClient && this.stompClient.connected) {
        resolve(this.stompClient)
        return
      }

      const socket = new SockJS('/ws')
      this.stompClient = Stomp.over(socket)
      this.stompClient.debug = () => {}

      this.stompClient.connect(
        {},
        () => {
          this.connected = true
          console.log('WebSocket 连接成功')
          
          this.subscriptions.forEach((handler, destination) => {
            this.subscribe(destination, handler)
          })
          
          resolve(this.stompClient)
        },
        (error) => {
          console.error('WebSocket 连接失败:', error)
          this.connected = false
          
          setTimeout(() => {
            console.log('尝试重新连接...')
            this.connect()
          }, 5000)
          
          reject(error)
        }
      )
    })
  }

  subscribe(destination, handler) {
    this.subscriptions.set(destination, handler)
    
    if (this.stompClient && this.connected) {
      const subscription = this.stompClient.subscribe(destination, (message) => {
        try {
          const body = JSON.parse(message.body)
          handler(body)
        } catch (e) {
          handler(message.body)
        }
      })
      this.messageHandlers.set(destination, subscription)
    }
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect()
      this.connected = false
      console.log('WebSocket 已断开')
    }
  }
}

export default new WebSocketService()