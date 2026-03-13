Page({
  data: {
    roomCode: '',
    log: '未连接服务端'
  },

  onLoad() {
    // TODO: implement ws connect + auth(guest)
  },

  onRoomCode(e) {
    this.setData({ roomCode: e.detail.value });
  },

  onCreateRoom() {
    this.setData({ log: 'TODO: 创建房间（将走后端 REST）' });
  },

  onJoinRoom() {
    this.setData({ log: `TODO: 加入房间 ${this.data.roomCode}` });
  }
});
