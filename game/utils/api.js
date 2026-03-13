const BASE = 'http://127.0.0.1:18080'; // 本地开发：用端口转发或同机运行时可改

function post(path, data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE + path,
      method: 'POST',
      data,
      header: { 'content-type': 'application/json' },
      success: res => resolve(res.data),
      fail: err => reject(err)
    });
  });
}

function get(path) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE + path,
      method: 'GET',
      success: res => resolve(res.data),
      fail: err => reject(err)
    });
  });
}

module.exports = { BASE, post, get };
