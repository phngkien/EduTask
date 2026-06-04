import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { CreditCard, Check, ShieldCheck, ArrowLeft, Loader2, Sparkles, RefreshCw } from 'lucide-react';

export default function SubscriptionView({ user, onBack }) {
  const [plans, setPlans] = useState([]);
  const [activeSub, setActiveSub] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Selected plan for checkout modal
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('CREDIT_CARD');
  const [processingPayment, setProcessingPayment] = useState(false);
  const [checkoutSuccess, setCheckoutSuccess] = useState(false);

  const loadSubscriptionData = async () => {
    setLoading(true);
    try {
      const loadedPlans = await api.getPlans();
      setPlans(loadedPlans);

      const sub = await api.getActiveSubscription();
      setActiveSub(sub);

      const history = await api.getTransactions();
      setTransactions(history);
    } catch (error) {
      console.error('Failed to load subscription data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSubscriptionData();
  }, []);

  const handleCheckout = async (e) => {
    e.preventDefault();
    if (!selectedPlan) return;
    setProcessingPayment(true);
    try {
      // Gọi API Đăng ký gói cước thương mại
      await api.subscribe(selectedPlan.planId, selectedPlan.price, paymentMethod);
      setCheckoutSuccess(true);
      
      // Load lại thông tin
      const sub = await api.getActiveSubscription();
      setActiveSub(sub);

      const history = await api.getTransactions();
      setTransactions(history);
    } catch (error) {
      alert(error.message || 'Thanh toán thất bại, vui lòng thử lại');
    } finally {
      setProcessingPayment(false);
    }
  };

  const closeCheckout = () => {
    setSelectedPlan(null);
    setCheckoutSuccess(false);
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  };

  const getPlanIcon = (name) => {
    if (name === 'PRO') return '🔥';
    if (name === 'STUDENT') return '🎓';
    return '🌱';
  };

  return (
    <div className="sub-view-panel">
      <div className="sub-header-row">
        <button className="btn-back" onClick={onBack}>
          <ArrowLeft size={18} />
          <span>Quay lại</span>
        </button>
        <h2 className="sub-view-title">Nâng cấp Gói dịch vụ &amp; Thanh toán</h2>
      </div>

      {loading ? (
        <div className="sub-loading-state">
          <Loader2 className="spinner" size={32} />
          <p>Đang tải thông tin gói cước...</p>
        </div>
      ) : (
        <div className="sub-view-content">
          {/* Current Active Plan Status */}
          <div className="active-sub-card">
            <div className="active-sub-info">
              <span className="active-sub-icon">
                {activeSub ? getPlanIcon(activeSub.planName) : '🌱'}
              </span>
              <div>
                <h3>Gói dịch vụ hiện tại: <span className="plan-tag-highlight">{activeSub ? activeSub.planName : 'FREE'}</span></h3>
                {activeSub ? (
                  <p className="active-sub-date">
                    Thời hạn từ <strong>{new Date(activeSub.startDate).toLocaleDateString('vi-VN')}</strong> đến{' '}
                    <strong>{new Date(activeSub.endDate).toLocaleDateString('vi-VN')}</strong>
                  </p>
                ) : (
                  <p className="active-sub-date">Phiên bản miễn phí có giới hạn số lượng nhóm và thành viên.</p>
                )}
              </div>
            </div>
            {activeSub && activeSub.planName !== 'FREE' && (
              <div className="active-sub-badge">
                <ShieldCheck size={18} />
                <span>Đã kích hoạt</span>
              </div>
            )}
          </div>

          {/* Available Plans Grid */}
          <div className="plans-section">
            <h3 className="section-title">Chọn gói dịch vụ phù hợp</h3>
            <div className="plans-grid">
              {plans.map((plan) => {
                const isActive = (activeSub && activeSub.planId === plan.planId) || (!activeSub && plan.planName === 'FREE');
                const isPro = plan.planName === 'PRO';

                return (
                  <div key={plan.planId} className={`plan-card-item ${isPro ? 'pro-tier' : ''} ${isActive ? 'active-tier' : ''}`}>
                    {isPro && <div className="popular-badge"><Sparkles size={12} /> Bán chạy nhất</div>}
                    <div className="plan-card-header">
                      <span className="plan-emoji">{getPlanIcon(plan.planName)}</span>
                      <h4 className="plan-card-name">{plan.planName}</h4>
                      <div className="plan-card-price">
                        <span className="price-amount">{formatCurrency(plan.price)}</span>
                        <span className="price-period"> / {plan.durationDays} ngày</span>
                      </div>
                    </div>

                    <div className="plan-divider"></div>

                    <ul className="plan-features-list">
                      {plan.features.split(',').map((feat, idx) => (
                        <li key={idx} className="plan-feature-item">
                          <Check size={16} className="check-icon" />
                          <span>{feat.trim()}</span>
                        </li>
                      ))}
                    </ul>

                    <button
                      className={`plan-action-btn ${isActive ? 'btn-current' : isPro ? 'btn-upgrade-pro' : 'btn-upgrade'}`}
                      onClick={() => !isActive && setSelectedPlan(plan)}
                      disabled={isActive}
                    >
                      {isActive ? 'Đang hoạt động' : 'Nâng cấp ngay'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Transactions History Section */}
          <div className="transactions-section">
            <h3 className="section-title">Lịch sử giao dịch</h3>
            <div className="table-responsive">
              {transactions.length === 0 ? (
                <div className="empty-transactions">
                  <p>Bạn chưa có giao dịch thanh toán nào trước đây.</p>
                </div>
              ) : (
                <table className="transactions-table">
                  <thead>
                    <tr>
                      <th>Mã Giao dịch</th>
                      <th>Gói cước</th>
                      <th>Số tiền</th>
                      <th>Phương thức</th>
                      <th>Ngày thanh toán</th>
                      <th>Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map((tx) => (
                      <tr key={tx.transactionId}>
                        <td>#TX-{tx.transactionId}</td>
                        <td className="tx-plan-name">{tx.planName}</td>
                        <td className="tx-amount">{formatCurrency(tx.amount)}</td>
                        <td className="tx-method">{tx.paymentMethod === 'CREDIT_CARD' ? 'Thẻ tín dụng' : tx.paymentMethod === 'E_WALLET' ? 'Ví điện tử' : 'Chuyển khoản'}</td>
                        <td>{new Date(tx.createdAt).toLocaleDateString('vi-VN')} {new Date(tx.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</td>
                        <td>
                          <span className={`tx-status ${tx.status.toLowerCase()}`}>
                            {tx.status === 'SUCCESS' ? 'Thành công' : 'Thất bại'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Checkout Modal */}
      {selectedPlan && (
        <div className="modal-overlay">
          <div className="modal-content checkout-modal-content">
            <div className="modal-header">
              <h3 className="modal-title">Xác nhận nâng cấp dịch vụ</h3>
              <button className="modal-close-btn" onClick={closeCheckout}>✕</button>
            </div>

            {checkoutSuccess ? (
              <div className="modal-body checkout-success-body">
                <div className="success-icon-box">
                  <ShieldCheck size={48} />
                </div>
                <h3>Nâng cấp thành công!</h3>
                <p>
                  Gói dịch vụ của bạn đã được nâng cấp lên <strong>{selectedPlan.planName}</strong>. Thời hạn sử dụng thêm{' '}
                  <strong>{selectedPlan.durationDays} ngày</strong>.
                </p>
                <button className="btn-primary" style={{ width: '100%', marginTop: '1rem' }} onClick={closeCheckout}>
                  Đóng &amp; Tiếp tục
                </button>
              </div>
            ) : (
              <form onSubmit={handleCheckout}>
                <div className="modal-body">
                  <div className="checkout-summary-box">
                    <div className="checkout-row">
                      <span>Gói cước đăng ký:</span>
                      <strong>Gói {selectedPlan.planName}</strong>
                    </div>
                    <div className="checkout-row">
                      <span>Thời hạn sử dụng:</span>
                      <strong>{selectedPlan.durationDays} ngày</strong>
                    </div>
                    <div className="checkout-row divider"></div>
                    <div className="checkout-row total">
                      <span>Tổng cộng cần thanh toán:</span>
                      <span className="total-price">{formatCurrency(selectedPlan.price)}</span>
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Chọn phương thức thanh toán</label>
                    <div className="payment-options-grid">
                      <label className={`payment-option ${paymentMethod === 'CREDIT_CARD' ? 'selected' : ''}`}>
                        <input
                          type="radio"
                          name="payMethod"
                          value="CREDIT_CARD"
                          checked={paymentMethod === 'CREDIT_CARD'}
                          onChange={(e) => setPaymentMethod(e.target.value)}
                        />
                        <div className="payment-opt-content">
                          <CreditCard size={18} />
                          <span>Thẻ tín dụng / Quốc tế</span>
                        </div>
                      </label>

                      <label className={`payment-option ${paymentMethod === 'E_WALLET' ? 'selected' : ''}`}>
                        <input
                          type="radio"
                          name="payMethod"
                          value="E_WALLET"
                          checked={paymentMethod === 'E_WALLET'}
                          onChange={(e) => setPaymentMethod(e.target.value)}
                        />
                        <div className="payment-opt-content">
                          <span>📱</span>
                          <span>Ví MoMo / E-Wallet</span>
                        </div>
                      </label>

                      <label className={`payment-option ${paymentMethod === 'BANK_TRANSFER' ? 'selected' : ''}`}>
                        <input
                          type="radio"
                          name="payMethod"
                          value="BANK_TRANSFER"
                          checked={paymentMethod === 'BANK_TRANSFER'}
                          onChange={(e) => setPaymentMethod(e.target.value)}
                        />
                        <div className="payment-opt-content">
                          <span>🏦</span>
                          <span>Chuyển khoản Ngân hàng</span>
                        </div>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="modal-footer">
                  <button type="button" className="btn-secondary" onClick={closeCheckout} disabled={processingPayment}>
                    Hủy bỏ
                  </button>
                  <button type="submit" className="btn-primary btn-checkout-submit" disabled={processingPayment}>
                    {processingPayment ? (
                      <>
                        <RefreshCw className="spinner" size={16} />
                        <span>Đang xử lý giao dịch...</span>
                      </>
                    ) : (
                      <span>Xác nhận &amp; Thanh toán</span>
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
