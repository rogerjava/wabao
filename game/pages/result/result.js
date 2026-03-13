Page({
  data: { won: false },
  onLoad(q) {
    this.setData({ won: String(q.won) === '1' });
  },
  goHome() {
    wx.reLaunch({ url: '/pages/index/index' });
  }
});
