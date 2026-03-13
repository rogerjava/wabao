const api = require('../../utils/api');
const store = require('../../utils/store');

Page({
  data: {
    roomCode: '',
    log: '准备就绪',
    nickname: '玩家' + Math.floor(Math.random() * 1000)
  },

  async ensureSession() {
    let s = store.loadSession();
    if (s && s.sessionToken) return s;
    const resp = await api.post('/api/auth/guest', { nickname: this.data.nickname });
    store.saveSession(resp);
    return resp;
  },

  onRoomCode(e) {
    this.setData({ roomCode: e.detail.value.trim().toUpperCase() });
  },

  onNickname(e) {
    this.setData({ nickname: e.detail.value.trim() });
  },

  async onCreateRoom() {
    try {
      const s = await this.ensureSession();
      const resp = await api.post('/api/rooms', {
        sessionToken: s.sessionToken,
        mode: 'COOP',
        difficulty: 'EASY'
      });
      wx.navigateTo({ url: `/pages/room/room?code=${resp.roomCode}` });
    } catch (e) {
      this.setData({ log: '创建房间失败：' + (e.errMsg || e) });
    }
  },

  async onJoinRoom() {
    try {
      const code = this.data.roomCode;
      if (!code) return this.setData({ log: '请输入房间号' });
      const s = await this.ensureSession();
      await api.post(`/api/rooms/${code}/join`, { sessionToken: s.sessionToken });
      wx.navigateTo({ url: `/pages/room/room?code=${code}` });
    } catch (e) {
      this.setData({ log: '加入失败：' + (e.errMsg || e) });
    }
  }
});
