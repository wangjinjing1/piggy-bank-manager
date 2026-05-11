const api = async (url, options = {}) => {
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(url, { ...options, headers });
  const json = await res.json();
  if (!json.success) throw new Error(json.message || '请求失败');
  return json.data;
};

const queryString = (query) => new URLSearchParams(Object.entries(query).filter(([, value]) => value)).toString();
const pad2 = (value) => String(value).padStart(2, '0');
const formatDateValue = (value) => {
  if (!value) return '';
  const text = String(value).slice(0, 10);
  const match = text.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
  if (!match) return value;
  return `${match[1]}年${pad2(match[2])}月${pad2(match[3])}日`;
};
const formatDateTimeValue = (value) => {
  if (!value) return '';
  const text = String(value).replace('T', ' ');
  const date = formatDateValue(text.slice(0, 10));
  const time = text.slice(11, 19);
  return time ? `${date} ${time}` : date;
};

Vue.createApp({
  data() {
    const today = new Date().toISOString().slice(0, 10);
    return {
      token: localStorage.getItem('token') || '',
      me: JSON.parse(localStorage.getItem('me') || '{}'),
      view: 'borrow',
      titles: { borrow: '借账单', overdue: '逾期借条', deposit: '存账单', report: '统计导出', users: '用户管理', blacklist: 'IP 黑名单' },
      error: '', notice: '',
      loginForm: { username: '', password: '' },
      borrows: [], overdueGroups: [], deposits: [], users: [], blacklists: [],
      borrowQuery: { borrowerName: '', phone: '', email: '', borrowStartDate: '', borrowEndDate: '', dueStartDate: '', dueEndDate: '' },
      depositQuery: { bank: '', depositStartDate: '', depositEndDate: '', dueStartDate: '', dueEndDate: '' },
      showBorrow: false, showDeposit: false, showUser: false,
      borrowForm: { id: null, borrowerName: '', status: 'NORMAL', phone: '', email: '', amount: null, borrowDate: today, dueDate: '9999-12-31' },
      depositForm: { id: null, amount: null, bank: '', depositDate: today, dueDate: today, status: 'NORMAL' },
      userForm: { userId: null, username: '', password: '', email: '' },
      reportQuery: { type: 'BORROW', startDate: '', endDate: '9999-12-31', name: '' },
      report: { total: 0, items: [] }
    };
  },
  async mounted() {
    if (this.token) await this.refresh();
  },
  methods: {
    async run(fn) {
      this.error = ''; this.notice = '';
      try { return await fn(); } catch (e) { this.error = e.message; }
    },
    async login() {
      await this.run(async () => {
        const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify(this.loginForm) });
        this.token = data.token; this.me = data;
        localStorage.setItem('token', data.token); localStorage.setItem('me', JSON.stringify(data));
        await this.refresh();
      });
    },
    logout() { localStorage.clear(); this.token = ''; this.me = {}; },
    async refresh() { await Promise.all([this.loadBorrows(), this.loadDeposits()]); },
    async ensureUsersForAdmin() {
      if (this.me.role === 'ADMIN' && this.users.length === 0) {
        this.users = await api('/api/users');
      }
    },
    async loadBorrows() {
      await this.ensureUsersForAdmin();
      this.borrows = await api(`/api/borrows?${queryString(this.borrowQuery)}`);
    },
    async loadOverdue() { this.overdueGroups = await api('/api/borrows/overdue'); },
    async loadDeposits() {
      await this.ensureUsersForAdmin();
      this.deposits = await api(`/api/deposits?${queryString(this.depositQuery)}`);
    },
    async loadUsers() { this.users = await api('/api/users'); },
    async loadBlacklist() { this.blacklists = await api('/api/security/blacklist'); },
    async resetBorrowQuery() {
      this.borrowQuery = { borrowerName: '', phone: '', email: '', borrowStartDate: '', borrowEndDate: '', dueStartDate: '', dueEndDate: '' };
      await this.loadBorrows();
    },
    async resetDepositQuery() {
      this.depositQuery = { bank: '', depositStartDate: '', depositEndDate: '', dueStartDate: '', dueEndDate: '' };
      await this.loadDeposits();
    },
    openBorrow(borrow) {
      const today = new Date().toISOString().slice(0, 10);
      this.borrowForm = borrow
        ? {
            id: borrow.id,
            borrowerName: borrow.borrowerName,
            status: borrow.status,
            phone: borrow.phone,
            email: borrow.email,
            amount: borrow.amount,
            borrowDate: String(borrow.borrowDate || today).slice(0, 10),
            dueDate: String(borrow.dueDate || '9999-12-31').slice(0, 10)
          }
        : { id: null, borrowerName: '', status: 'NORMAL', phone: '', email: '', amount: null, borrowDate: today, dueDate: '9999-12-31' };
      this.showBorrow = true;
    },
    openDeposit(deposit) {
      const today = new Date().toISOString().slice(0, 10);
      this.depositForm = deposit
        ? {
            id: deposit.id,
            amount: deposit.amount,
            bank: deposit.bank,
            depositDate: String(deposit.depositDate || today).slice(0, 10),
            dueDate: String(deposit.dueDate || today).slice(0, 10),
            status: deposit.status
          }
        : { id: null, amount: null, bank: '', depositDate: today, dueDate: today, status: 'NORMAL' };
      this.showDeposit = true;
    },
    openUser(user) {
      this.userForm = user
        ? { userId: user.userId, username: user.username, password: '', email: user.email || '' }
        : { userId: null, username: '', password: '', email: '' };
      this.showUser = true;
    },
    async saveBorrow() {
      await this.run(async () => {
        const body = { ...this.borrowForm, dueDate: this.borrowForm.dueDate || null };
        const url = this.borrowForm.id ? `/api/borrows/${this.borrowForm.id}` : '/api/borrows';
        const method = this.borrowForm.id ? 'PUT' : 'POST';
        await api(url, { method, body: JSON.stringify(body) });
        this.showBorrow = false; await this.loadBorrows();
      });
    },
    async saveDeposit() {
      await this.run(async () => {
        const url = this.depositForm.id ? `/api/deposits/${this.depositForm.id}` : '/api/deposits';
        const method = this.depositForm.id ? 'PUT' : 'POST';
        await api(url, { method, body: JSON.stringify(this.depositForm) });
        this.showDeposit = false; await this.loadDeposits();
      });
    },
    async saveUser() {
      await this.run(async () => {
        if (this.userForm.userId) {
          await api(`/api/users/${this.userForm.userId}`, { method: 'PUT', body: JSON.stringify(this.userForm) });
          if (this.userForm.userId === this.me.userId) {
            this.me.username = this.userForm.username;
            localStorage.setItem('me', JSON.stringify(this.me));
          }
        } else {
          await api('/api/users', { method: 'POST', body: JSON.stringify(this.userForm) });
        }
        this.showUser = false;
        this.userForm = { userId: null, username: '', password: '', email: '' };
        await this.loadUsers();
      });
    },
    async createLink() {
      await this.run(async () => {
        const data = await api('/api/borrows/links', { method: 'POST', body: '{}' });
        this.notice = `匿名填写链接：${data.url}`;
      });
    },
    async approveBorrow(id) { await this.run(async () => { await api(`/api/borrows/${id}/approve`, { method: 'PATCH', body: '{}' }); await this.loadBorrows(); }); },
    async setBorrowStatus(b, status) { await this.run(async () => { await api(`/api/borrows/${b.id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }); await this.loadBorrows(); }); },
    async changeBorrowOwner(b) { await this.run(async () => { await api(`/api/borrows/${b.id}/owner`, { method: 'PATCH', body: JSON.stringify({ ownerUserId: b.ownerUserId }) }); await this.loadBorrows(); }); },
    async deleteBorrow(id) {
      if (!confirm('确认删除这条借账单吗？')) return;
      await this.run(async () => { await api(`/api/borrows/${id}`, { method: 'DELETE' }); await this.loadBorrows(); });
    },
    async sendOverdueReminder(email) { await this.run(async () => { await api('/api/borrows/overdue/remind', { method: 'POST', body: JSON.stringify({ email }) }); await this.loadOverdue(); }); },
    async setDepositStatus(d, status) { await this.run(async () => { await api(`/api/deposits/${d.id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }); await this.loadDeposits(); }); },
    async changeDepositOwner(d) { await this.run(async () => { await api(`/api/deposits/${d.id}/owner`, { method: 'PATCH', body: JSON.stringify({ ownerUserId: d.ownerUserId }) }); await this.loadDeposits(); }); },
    async deleteDeposit(id) {
      if (!confirm('确认删除这条存账单吗？')) return;
      await this.run(async () => { await api(`/api/deposits/${id}`, { method: 'DELETE' }); await this.loadDeposits(); });
    },
    async removeBlacklist(id) { await this.run(async () => { await api(`/api/security/blacklist/${id}`, { method: 'DELETE' }); await this.loadBlacklist(); }); },
    async loadReport() {
      await this.run(async () => {
        this.report = await api(`/api/reports?${queryString(this.reportQuery)}`);
      });
    },
    async exportReport() {
      await this.run(async () => {
        const res = await fetch(`/api/reports/export?${queryString(this.reportQuery)}`, { headers: { Authorization: `Bearer ${this.token}` } });
        if (!res.ok) throw new Error('导出失败');
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.reportQuery.type === 'DEPOSIT' ? 'deposit-report.csv' : 'borrow-report.csv';
        a.click();
        URL.revokeObjectURL(url);
      });
    },
    money(v) { return Number(v || 0).toLocaleString('zh-CN', { style: 'currency', currency: 'CNY' }); },
    formatDate(v) { return formatDateValue(v); },
    formatDateTime(v) { return formatDateTimeValue(v); },
    statusText(s) { return s === 'VOID' ? '作废' : '正常'; },
    auditText(s) { return ({ APPROVED: '已通过', PENDING: '待审核', REJECTED: '已拒绝' })[s] || s; },
    mailStatusText(s) { return ({ NOT_SENT: '未发送', SENT: '已发送', FAILED: '发送失败' })[s] || '未发送'; }
  }
}).mount('#app');
