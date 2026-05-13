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

// 单页应用的全部状态和页面动作都集中在这里，方便排查前端交互问题。
Vue.createApp({
  data() {
    const today = new Date().toISOString().slice(0, 10);
    return {
      token: localStorage.getItem('token') || '',
      me: JSON.parse(localStorage.getItem('me') || '{}'),
      view: 'borrow',
      titles: { borrow: '借账单', overdue: '逾期借条', deposit: '存账单', report: '统计导出', users: '用户管理', blacklist: 'IP 黑名单' },
      error: '',
      showMessage: false, messageTitle: '', messageText: '', messageCopyText: '',
      loginForm: { username: '', password: '' },
      borrows: [], overdueGroups: [], deposits: [], users: [], blacklists: [],
      borrowPage: { page: 1, size: 5, total: 0 },
      depositPage: { page: 1, size: 5, total: 0 },
      borrowQuery: { borrowerName: '', phone: '', email: '', borrowStartDate: '', borrowEndDate: '', dueStartDate: '', dueEndDate: '', page: 1, size: 5 },
      depositQuery: { depositorName: '', bank: '', depositStartDate: '', depositEndDate: '', dueStartDate: '', dueEndDate: '', page: 1, size: 5 },
      showBorrow: false, showDeposit: false, showUser: false, showRepay: false, showWithdraw: false, showDetail: false,
      activeBill: null,
      detailType: 'BORROW',
      detailPage: 1,
      detail: {},
      borrowForm: { id: null, ownerUserId: null, borrowerName: '', status: 'NORMAL', phone: '', email: '', amount: null, borrowDate: today, dueDate: '9999-12-31', remark: '' },
      repayForm: { amount: null, repaymentDate: today, remark: '' },
      depositForm: { id: null, ownerUserId: null, depositorName: '', billType: 'DEPOSIT', amount: null, bank: '', depositDate: today, dueDate: '9999-12-31', status: 'NORMAL', remark: '' },
      withdrawForm: { depositorName: '', amount: null, withdrawalDate: today, remark: '' },
      userForm: { userId: null, username: '', password: '', email: '' },
      reportQuery: { type: 'BORROW', startDate: '', endDate: '9999-12-31', name: '', page: 1, size: 10 },
      report: { total: 0, groups: { items: [], total: 0, page: 1, size: 10 } }
    };
  },
  async mounted() {
    if (this.token) await this.refresh();
  },
  computed: {
    detailTitle() {
      if (this.detailType === 'DEPOSIT') return '存账单详情';
      return '借账单详情';
    },
    detailPageData() {
      return this.detail.repayments || this.detail.withdrawals || this.detail.records || { items: [], total: 0, size: 10 };
    },
    detailRecords() {
      const rows = this.detailPageData.items || [];
      return rows.map((row) => ({
        ...row,
        amount: row.amount || row.remainingAmount || 0,
        repaymentDate: row.repaymentDate || row.borrowDate || row.depositDate,
        withdrawalDate: row.withdrawalDate,
        remark: row.remark || row.bank || row.email || ''
      }));
    }
  },
  methods: {
    async run(fn) {
      this.error = '';
      try { return await fn(); } catch (e) { this.error = e.message; this.openMessage('提示', e.message || '操作失败'); }
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
      const data = await api(`/api/borrows?${queryString(this.borrowQuery)}`);
      this.borrowPage = { page: data.page, size: data.size, total: data.total };
      this.borrows = data.items || [];
    },
    async loadOverdue() { this.overdueGroups = await api('/api/borrows/overdue'); },
    async loadDeposits() {
      await this.ensureUsersForAdmin();
      const data = await api(`/api/deposits?${queryString(this.depositQuery)}`);
      this.depositPage = { page: data.page, size: data.size, total: data.total };
      this.deposits = data.items || [];
    },
    async loadUsers() { this.users = await api('/api/users'); },
    async loadBlacklist() { this.blacklists = await api('/api/security/blacklist'); },
    async resetBorrowQuery() {
      this.borrowQuery = { borrowerName: '', phone: '', email: '', borrowStartDate: '', borrowEndDate: '', dueStartDate: '', dueEndDate: '', page: 1, size: 5 };
      await this.loadBorrows();
    },
    async resetDepositQuery() {
      this.depositQuery = { depositorName: '', bank: '', depositStartDate: '', depositEndDate: '', dueStartDate: '', dueEndDate: '', page: 1, size: 5 };
      await this.loadDeposits();
    },
    async changeBorrowPage(delta) {
      const next = this.borrowQuery.page + delta;
      if (next < 1 || next > this.pageCount(this.borrowPage.total, this.borrowQuery.size)) return;
      this.borrowQuery.page = next;
      await this.loadBorrows();
    },
    async changeDepositPage(delta) {
      const next = this.depositQuery.page + delta;
      if (next < 1 || next > this.pageCount(this.depositPage.total, this.depositQuery.size)) return;
      this.depositQuery.page = next;
      await this.loadDeposits();
    },
    openBorrow(borrow) {
      const today = new Date().toISOString().slice(0, 10);
      this.borrowForm = borrow
        ? {
            id: borrow.id,
            ownerUserId: borrow.ownerUserId,
            borrowerName: borrow.borrowerName,
            status: borrow.status,
            phone: borrow.phone,
            email: borrow.email,
            amount: borrow.amount,
            borrowDate: String(borrow.borrowDate || today).slice(0, 10),
            dueDate: String(borrow.dueDate || '9999-12-31').slice(0, 10),
            remark: borrow.remark || ''
          }
        : { id: null, ownerUserId: null, borrowerName: '', status: 'NORMAL', phone: '', email: '', amount: null, borrowDate: today, dueDate: '9999-12-31', remark: '' };
      this.showBorrow = true;
    },
    openDeposit(deposit) {
      const today = new Date().toISOString().slice(0, 10);
      this.depositForm = deposit
        ? {
            id: deposit.id,
            ownerUserId: deposit.ownerUserId,
            depositorName: deposit.depositorName,
            billType: deposit.billType || (Number(deposit.amount) < 0 ? 'WITHDRAW' : 'DEPOSIT'),
            amount: deposit.amount,
            bank: deposit.bank,
            depositDate: String(deposit.depositDate || today).slice(0, 10),
            dueDate: String(deposit.dueDate || '9999-12-31').slice(0, 10),
            status: deposit.status,
            remark: deposit.remark || ''
          }
        : { id: null, ownerUserId: null, depositorName: '', billType: 'DEPOSIT', amount: null, bank: '', depositDate: today, dueDate: '9999-12-31', status: 'NORMAL', remark: '' };
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
        const isEdit = !!this.borrowForm.id;
        const { ownerUserId, ...form } = this.borrowForm;
        const body = { ...form, dueDate: this.borrowForm.dueDate || null };
        const url = isEdit ? `/api/borrows/${this.borrowForm.id}` : '/api/borrows';
        const method = isEdit ? 'PUT' : 'POST';
        await api(url, { method, body: JSON.stringify(body) });
        if (isEdit && this.me.role === 'ADMIN' && this.borrowForm.ownerUserId) {
          await api(`/api/borrows/${this.borrowForm.id}/owner`, { method: 'PATCH', body: JSON.stringify({ ownerUserId: this.borrowForm.ownerUserId }) });
        }
        this.showBorrow = false; await this.loadBorrows();
      });
    },
    async saveDeposit() {
      await this.run(async () => {
        const isEdit = !!this.depositForm.id;
        const url = isEdit ? `/api/deposits/${this.depositForm.id}` : '/api/deposits';
        const method = isEdit ? 'PUT' : 'POST';
        const { ownerUserId, ...body } = this.depositForm;
        await api(url, { method, body: JSON.stringify(body) });
        if (isEdit && this.me.role === 'ADMIN' && this.depositForm.ownerUserId) {
          await api(`/api/deposits/${this.depositForm.id}/owner`, { method: 'PATCH', body: JSON.stringify({ ownerUserId: this.depositForm.ownerUserId }) });
        }
        this.showDeposit = false; await this.loadDeposits();
      });
    },
    openRepay(bill) {
      const today = new Date().toISOString().slice(0, 10);
      this.activeBill = bill;
      this.repayForm = { amount: null, repaymentDate: today, remark: '' };
      this.showRepay = true;
    },
    async saveRepay() {
      await this.run(async () => {
        await api(`/api/borrows/${this.activeBill.id}/repayments`, { method: 'POST', body: JSON.stringify(this.repayForm) });
        this.showRepay = false;
        await this.loadBorrows();
      });
    },
    openWithdraw() {
      const today = new Date().toISOString().slice(0, 10);
      this.activeBill = null;
      this.withdrawForm = { depositorName: '', amount: null, withdrawalDate: today, remark: '' };
      this.showWithdraw = true;
    },
    async saveWithdraw() {
      await this.run(async () => {
        await api('/api/deposits/withdrawals', { method: 'POST', body: JSON.stringify(this.withdrawForm) });
        this.showWithdraw = false;
        await this.loadDeposits();
        this.openMessage('操作成功', '取钱已保存，并新增一条负数存账单记录');
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
        this.openMessage('匿名填写链接', data.url, data.url);
      });
    },
    async createWithdrawalLink() {
      await this.run(async () => {
        const data = await api('/api/deposits/withdrawal-links', { method: 'POST', body: '{}' });
        this.openMessage('匿名取钱链接', data.url, data.url);
      });
    },
    openMessage(title, text, copyText = '') {
      this.messageTitle = title;
      this.messageText = text;
      this.messageCopyText = copyText;
      this.showMessage = true;
    },
    async copyMessage() {
      if (!this.messageCopyText) return;
      try {
        await navigator.clipboard.writeText(this.messageCopyText);
        this.messageTitle = '已复制';
      } catch (e) {
        this.messageTitle = '复制失败';
      }
    },
    async approveBorrow(id) { await this.run(async () => { await api(`/api/borrows/${id}/approve`, { method: 'PATCH', body: '{}' }); await this.loadBorrows(); }); },
    async setBorrowStatus(b, status) { await this.run(async () => { await api(`/api/borrows/${b.id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }); await this.loadBorrows(); }); },
    async deleteBorrow(id) {
      if (!confirm('确认删除这条借账单吗？')) return;
      await this.run(async () => { await api(`/api/borrows/${id}`, { method: 'DELETE' }); await this.loadBorrows(); });
    },
    async sendOverdueReminder(email) { await this.run(async () => { await api('/api/borrows/overdue/remind', { method: 'POST', body: JSON.stringify({ email }) }); await this.loadOverdue(); }); },
    async approveWithdrawal(id) { await this.run(async () => { await api(`/api/deposits/${id}/approve-withdrawal`, { method: 'PATCH', body: '{}' }); await this.loadDeposits(); }); },
    async setDepositStatus(d, status) { await this.run(async () => { await api(`/api/deposits/${d.id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }); await this.loadDeposits(); }); },
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
    async changeReportType() {
      this.reportQuery.page = 1;
      this.report = { total: 0, groups: { items: [], total: 0, page: 1, size: this.reportQuery.size } };
      await this.loadReport();
    },
    async changeReportPage(delta) {
      const next = this.reportQuery.page + delta;
      if (next < 1 || next > this.pageCount(this.report.groups?.total, this.reportQuery.size)) return;
      this.reportQuery.page = next;
      await this.loadReport();
    },
    async openReportDetail(group) {
      this.detailType = this.reportQuery.type;
      this.detailPage = 1;
      this.detail = {
        bill: { amount: group.total, remainingAmount: group.total, remark: `${group.name || '-'} 共 ${group.count} 笔` },
        records: { items: group.items, total: group.items.length, size: group.items.length || 1 }
      };
      this.showDetail = true;
    },
    async openBorrowDetail(bill) {
      this.detailType = 'BORROW';
      this.activeBill = bill;
      this.detailPage = 1;
      await this.loadDetail();
      this.showDetail = true;
    },
    async openDepositDetail(bill) {
      this.detailType = 'DEPOSIT';
      this.activeBill = bill;
      this.detailPage = 1;
      await this.loadDetail();
      this.showDetail = true;
    },
    async loadDetail() {
      const base = this.detailType === 'BORROW' ? 'borrows' : 'deposits';
      this.detail = await api(`/api/${base}/${this.activeBill.id}?page=${this.detailPage}&size=10`);
    },
    async deleteRepayment(record) {
      if (this.detailType !== 'BORROW') return;
      if (!confirm('确认删除这条还款记录吗？')) return;
      await this.run(async () => {
        await api(`/api/borrows/${this.activeBill.id}/repayments/${record.id}`, { method: 'DELETE' });
        await this.loadDetail();
        await this.loadBorrows();
      });
    },
    async sendAuditMailFromDetail() {
      if (!this.detail.bill?.id) return;
      await this.run(async () => {
        this.detail.bill = await api(`/api/borrows/${this.detail.bill.id}/audit-mail`, { method: 'POST', body: '{}' });
        await this.loadBorrows();
      });
    },
    async changeDetailPage(delta) {
      const pageData = this.detailPageData;
      const next = this.detailPage + delta;
      if (next < 1 || next > this.pageCount(pageData.total, pageData.size)) return;
      this.detailPage = next;
      await this.loadDetail();
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
    usernameById(id) {
      if (!id) return '-';
      if (Number(id) === Number(this.me.userId)) return this.me.username || '-';
      return (this.users.find((user) => Number(user.userId) === Number(id)) || {}).username || `用户 ${id}`;
    },
    pageCount(total, size) { return Math.max(Math.ceil(Number(total || 0) / Number(size || 10)), 1); },
    formatDate(v) { return formatDateValue(v); },
    formatDateTime(v) { return formatDateTimeValue(v); },
    statusText(s) { return s === 'VOID' ? '作废' : '正常'; },
    depositBillTypeText(type, amount) { return type === 'WITHDRAW' || Number(amount) < 0 ? '取钱' : '存钱'; },
    auditText(s) { return ({ APPROVED: '已通过', PENDING: '待审核', REJECTED: '已拒绝' })[s] || s; },
    mailStatusText(s) { return ({ NOT_SENT: '未发送', SENT: '已发送', FAILED: '发送失败' })[s] || '未发送'; }
  }
}).mount('#app');
