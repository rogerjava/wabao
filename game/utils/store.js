const KEY = 'wabao_session';

function saveSession(session) {
  wx.setStorageSync(KEY, session);
}

function loadSession() {
  return wx.getStorageSync(KEY) || null;
}

module.exports = { saveSession, loadSession };
