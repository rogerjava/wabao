const api = require('../../utils/api');
const store = require('../../utils/store');

Page({
  data: {
    code: '',
    rows: 10,
    cols: 10,
    teamHp: 3,
    timeLeft: 180,
    flagMode: false,
    canvasW: 320,
    canvasH: 320
  },

  onLoad(q) {
    this.setData({
      code: q.code,
      rows: Number(q.rows),
      cols: Number(q.cols),
      teamHp: Number(q.hp),
      timeLeft: Number(q.time)
    });

    this.initBoard();
    this.connectWs();
    this.startTimer();
  },

  onUnload() {
    if (this.timer) clearInterval(this.timer);
    if (this.ws) this.ws.close();
  },

  initBoard() {
    const { rows, cols } = this.data;
    this.board = Array.from({ length: rows }, () => Array.from({ length: cols }, () => ({ s: 'C' })));
  },

  startTimer() {
    this.timer = setInterval(() => {
      let t = this.data.timeLeft - 1;
      if (t < 0) t = 0;
      this.setData({ timeLeft: t });
      if (t === 0) {
        // MVP: 时间到由服务端判定，客户端这里只提示
      }
    }, 1000);
  },

  connectWs() {
    const s = store.loadSession();
    const url = `${api.BASE.replace('http','ws')}/ws?token=${encodeURIComponent(s.sessionToken)}&room=${encodeURIComponent(this.data.code)}`;
    const ws = wx.connectSocket({ url });
    this.ws = ws;

    ws.onOpen(() => {
      this.bindCanvas();
      this.draw();
    });

    ws.onMessage((evt) => {
      const msg = JSON.parse(evt.data);
      if (msg.type === 'CELL_UPDATE') {
        const p = msg.payload;
        this.setData({ teamHp: p.teamHp });
        for (const u of p.updates) {
          if (u.kind === 'DANGER') this.board[u.r][u.c] = { s: 'D' };
          if (u.kind === 'SAFE') this.board[u.r][u.c] = { s: 'O', n: u.number };
        }
        this.draw();
        if (p.won || p.lost) {
          wx.navigateTo({ url: `/pages/result/result?won=${p.won ? 1 : 0}` });
        }
      }
      if (msg.type === 'FLAG_UPDATE') {
        const p = msg.payload;
        this.board[p.r][p.c] = p.flag ? { s: 'F' } : { s: 'C' };
        this.draw();
      }
      if (msg.type === 'GAME_OVER') {
        const p = msg.payload;
        wx.navigateTo({ url: `/pages/result/result?won=${p.won ? 1 : 0}` });
      }
    });
  },

  send(type, payload) {
    if (!this.ws) return;
    this.ws.send({ data: JSON.stringify({ type, payload }) });
  },

  toggleFlagMode() {
    this.setData({ flagMode: !this.data.flagMode });
  },

  async bindCanvas() {
    const query = wx.createSelectorQuery();
    query.select('#board').fields({ node: true, size: true }).exec((res) => {
      const canvas = res[0].node;
      const ctx = canvas.getContext('2d');
      const dpr = wx.getSystemInfoSync().pixelRatio;
      const w = res[0].width;
      const h = res[0].height;
      canvas.width = w * dpr;
      canvas.height = h * dpr;
      ctx.scale(dpr, dpr);

      this.canvas = canvas;
      this.ctx = ctx;
      this.setData({ canvasW: w, canvasH: h });

      canvas.addEventListener('touchstart', (e) => {
        const touch = e.touches[0];
        const x = touch.x;
        const y = touch.y;
        this.onTouch(x, y);
      });

      this.draw();
    });
  },

  onTouch(x, y) {
    const { rows, cols, canvasW, canvasH, flagMode } = this.data;
    const cellW = canvasW / cols;
    const cellH = canvasH / rows;
    const c = Math.floor(x / cellW);
    const r = Math.floor(y / cellH);
    if (r < 0 || c < 0 || r >= rows || c >= cols) return;

    if (flagMode) {
      const cur = this.board[r][c];
      const nextFlag = cur.s !== 'F';
      this.send('FLAG_CELL', { r, c, flag: nextFlag });
    } else {
      this.send('OPEN_CELL', { r, c });
    }
  },

  draw() {
    if (!this.ctx) return;
    const { rows, cols, canvasW, canvasH } = this.data;
    const ctx = this.ctx;
    ctx.clearRect(0, 0, canvasW, canvasH);

    const cellW = canvasW / cols;
    const cellH = canvasH / rows;

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const x = c * cellW;
        const y = r * cellH;
        const cell = this.board[r][c];

        ctx.strokeStyle = '#bbb';
        ctx.strokeRect(x, y, cellW, cellH);

        if (cell.s === 'C') {
          ctx.fillStyle = '#d9c6a5'; // 宝箱底色
          ctx.fillRect(x+1, y+1, cellW-2, cellH-2);
        } else if (cell.s === 'F') {
          ctx.fillStyle = '#d9c6a5';
          ctx.fillRect(x+1, y+1, cellW-2, cellH-2);
          ctx.fillStyle = '#c0392b';
          ctx.font = `${Math.floor(cellH*0.6)}px sans-serif`;
          ctx.fillText('旗', x + cellW*0.25, y + cellH*0.75);
        } else if (cell.s === 'D') {
          ctx.fillStyle = '#333';
          ctx.fillRect(x+1, y+1, cellW-2, cellH-2);
          ctx.fillStyle = '#fff';
          ctx.font = `${Math.floor(cellH*0.6)}px sans-serif`;
          ctx.fillText('危', x + cellW*0.25, y + cellH*0.75);
        } else if (cell.s === 'O') {
          ctx.fillStyle = '#eee';
          ctx.fillRect(x+1, y+1, cellW-2, cellH-2);
          if (cell.n > 0) {
            ctx.fillStyle = '#2c3e50';
            ctx.font = `${Math.floor(cellH*0.6)}px sans-serif`;
            ctx.fillText(String(cell.n), x + cellW*0.35, y + cellH*0.75);
          }
        }
      }
    }
  }
});
