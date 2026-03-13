const api = require('../../utils/api');
const store = require('../../utils/store');

Page({
  data: {
    code: '',
    status: '',
    ownerId: '',
    players: [],
    ready: false,
    isOwner: false,
    log: ''
  },

  onLoad(q) {
    this.setData({ code: q.code });
    this.init();
  },

  async init() {
    const s = store.loadSession();
    if (!s) return this.setData({ log: '未登录' });

    // fetch room info
    const room = await api.get(`/api/rooms/${this.data.code}`);
    const isOwner = room.ownerId === s.playerId;
    const me = (room.players || []).find(p => p.playerId === s.playerId);

    this.setData({
      status: room.status,
      ownerId: room.ownerId,
      players: room.players,
      isOwner,
      ready: me ? me.ready : false
    });

    this.connectWs();
  },

  connectWs() {
    const s = store.loadSession();
    const url = `${api.BASE.replace('http','ws')}/ws?token=${encodeURIComponent(s.sessionToken)}&room=${encodeURIComponent(this.data.code)}`;
    const ws = wx.connectSocket({ url });
    this.ws = ws;

    ws.onOpen(() => {
      this.setData({ log: 'WS已连接' });
    });

    ws.onMessage((evt) => {
      const msg = JSON.parse(evt.data);
      if (msg.type === 'ROOM_STATE') {
        const room = msg.payload;
        const me = (room.players || []).find(p => p.playerId === s.playerId);
        this.setData({
          status: room.status,
          ownerId: room.ownerId,
          players: room.players,
          isOwner: room.ownerId === s.playerId,
          ready: me ? me.ready : false
        });
      }
      if (msg.type === 'GAME_INIT') {
        const g = msg.payload;
        wx.navigateTo({ url: `/pages/game/game?code=${this.data.code}&rows=${g.rows}&cols=${g.cols}&time=${g.timeLimitSec}&hp=${g.teamHp}` });
      }
    });

    ws.onClose(() => {
      this.setData({ log: 'WS断开（可返回重进）' });
    });
  },

  send(type, payload) {
    if (!this.ws) return;
    this.ws.send({ data: JSON.stringify({ type, payload }) });
  },

  onReady() {
    const next = !this.data.ready;
    this.send('READY', { ready: next });
  },

  onStart() {
    this.send('START_GAME', {});
  }
});
