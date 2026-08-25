let currentRunId = null;
let eventSource = null;

document.addEventListener('DOMContentLoaded', () => {
  initDefaults();
  setupEventListeners();
});

function initDefaults() {
  const deadlineInput = document.getElementById('deadline');
  if (deadlineInput && !deadlineInput.value) {
    const d = new Date(Date.now() + 4 * 60 * 60 * 1000);
    deadlineInput.value = d.toISOString().slice(0, 16);
  }
}

function setupEventListeners() {
  document.getElementById('btn-add-provider').addEventListener('click', () => addProviderRow());
  document.getElementById('btn-fill-example').addEventListener('click', fillExampleData);
  document.getElementById('btn-create-plan').addEventListener('click', handleCreatePlan);
  document.getElementById('btn-simulate-offline').addEventListener('click', handleSimulate);
  document.getElementById('btn-arm-live').addEventListener('click', handleArmLive);
  document.getElementById('btn-cancel-run').addEventListener('click', handleCancelRun);

  document.querySelectorAll('.chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const needText = btn.getAttribute('data-need');
      if (needText) {
        document.getElementById('need').value = needText;
      }
    });
  });
}

function addProviderRow(id = '', name = '', phone = '') {
  const container = document.getElementById('providers-container');
  const index = container.children.length + 1;
  const row = document.createElement('div');
  row.className = 'provider-row';
  row.innerHTML = `
    <input type="text" class="prov-id" placeholder="ID" value="${id || 'p' + index}">
    <input type="text" class="prov-name" placeholder="Provider Name" value="${name}">
    <input type="text" class="prov-phone" placeholder="+1..." value="${phone}">
    <button type="button" class="remove-btn" title="Remove">&times;</button>
  `;
  row.querySelector('.remove-btn').addEventListener('click', () => row.remove());
  container.appendChild(row);
}

function fillExampleData() {
  document.getElementById('stage').value = '6';
  document.getElementById('area').value = 'Sandton';
  document.getElementById('need').value = 'Emergency generator diesel refill and inverter switchover';
  document.getElementById('budget_amount').value = '1800.00';
  document.getElementById('budget_currency').value = 'ZAR';

  const container = document.getElementById('providers-container');
  container.innerHTML = '';
  addProviderRow('prov-fastspark', 'FastSpark Electrical', '+14155550101');
  addProviderRow('prov-poweron', 'PowerOn 24/7 Mobile', '+14155550102');
  addProviderRow('prov-sandton-inverter', 'Sandton Inverter Care', '+14155550103');
}

function getFormData() {
  const stage = parseInt(document.getElementById('stage').value, 10);
  const area = document.getElementById('area').value.trim();
  const need = document.getElementById('need').value.trim();
  const rawDeadline = document.getElementById('deadline').value;
  const deadline = new Date(rawDeadline).toISOString();
  const budgetAmount = parseFloat(document.getElementById('budget_amount').value);
  const budgetCurrency = document.getElementById('budget_currency').value.trim().toUpperCase();

  const providerRows = document.querySelectorAll('.provider-row');
  const providers = [];
  providerRows.forEach(row => {
    const id = row.querySelector('.prov-id').value.trim();
    const name = row.querySelector('.prov-name').value.trim();
    const phone = row.querySelector('.prov-phone').value.trim();
    if (id && name && phone) {
      providers.push({ id, name, phone_e164: phone });
    }
  });

  return {
    stage,
    area,
    need,
    deadline,
    budget_amount: budgetAmount,
    budget_currency: budgetCurrency,
    dry_run: true,
    providers
  };
}

async function handleCreatePlan() {
  const payload = getFormData();
  if (payload.providers.length === 0) {
    alert('Please specify at least one provider.');
    return;
  }

  try {
    const res = await fetch('/api/runs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      alert('Error creating plan: ' + JSON.stringify(err));
      return;
    }

    const run = await res.json();
    currentRunId = run.id;
    renderRun(run);
    subscribeSse(run.id);
  } catch (e) {
    alert('Network error creating plan: ' + e.message);
  }
}

async function handleSimulate() {
  try {
    const res = await fetch('/api/runs/simulate', {
      method: 'POST'
    });

    if (!res.ok) {
      const err = await res.json();
      alert('Simulation error: ' + JSON.stringify(err));
      return;
    }

    const run = await res.json();
    currentRunId = run.id;
    renderRun(run);
    subscribeSse(run.id);
  } catch (e) {
    alert('Network error starting simulation: ' + e.message);
  }
}

async function handleArmLive() {
  if (!currentRunId) return;

  if (!confirm('Gate 2 Consent: Do you confirm placing real live phone calls via CALL-E?')) {
    return;
  }

  try {
    const res = await fetch(`/api/runs/${currentRunId}/live`, {
      method: 'POST'
    });

    if (!res.ok) {
      const err = await res.json();
      alert('Error arming run: ' + (err.message || JSON.stringify(err)));
      return;
    }

    const run = await res.json();
    renderRun(run);
  } catch (e) {
    alert('Network error arming run: ' + e.message);
  }
}

async function handleCancelRun() {
  if (!currentRunId) return;

  try {
    const res = await fetch(`/api/runs/${currentRunId}/cancel`, {
      method: 'POST'
    });

    if (!res.ok) {
      const err = await res.json();
      alert('Error cancelling run: ' + JSON.stringify(err));
      return;
    }

    const run = await res.json();
    renderRun(run);
  } catch (e) {
    alert('Network error cancelling run: ' + e.message);
  }
}

function subscribeSse(runId) {
  if (eventSource) {
    eventSource.close();
  }

  eventSource = new EventSource(`/api/runs/${runId}/events`);
  eventSource.addEventListener('run_update', (event) => {
    try {
      const run = JSON.parse(event.data);
      renderRun(run);
    } catch (e) {
      console.error('Failed to parse SSE run_update payload', e);
    }
  });

  eventSource.onerror = () => {
    // Reconnect or close on terminal
  };
}

function renderRun(run) {
  document.getElementById('empty-state').style.display = 'none';
  document.getElementById('run-view').style.display = 'block';

  document.getElementById('run-id-display').textContent = run.id;
  document.getElementById('raw-json').textContent = JSON.stringify(run, null, 2);

  // Status Badge
  const statusBadge = document.getElementById('run-status-badge');
  statusBadge.className = `badge badge-${run.status.toLowerCase().replace('_', '-')}`;
  statusBadge.innerHTML = run.status === 'RUNNING' 
    ? `<span class="pulse-dot"></span> RUNNING`
    : run.status;

  // Controls
  const armBtn = document.getElementById('btn-arm-live');
  const cancelBtn = document.getElementById('btn-cancel-run');

  if (run.status === 'PLAN_READY') {
    armBtn.style.display = 'inline-flex';
    cancelBtn.style.display = 'none';
  } else if (run.status === 'RUNNING') {
    armBtn.style.display = 'none';
    cancelBtn.style.display = 'inline-flex';
  } else {
    armBtn.style.display = 'none';
    cancelBtn.style.display = 'none';
  }

  // Notice
  const notice = document.getElementById('run-notice');
  if (run.status === 'PLAN_READY') {
    notice.style.display = 'block';
    notice.textContent = 'Gate 1 active: Dry-run plan created. Phone numbers masked. No calls placed without explicit Gate 2 consent.';
  } else {
    notice.style.display = 'none';
  }

  // Winner Card
  const winnerContainer = document.getElementById('winner-container');
  if (run.status === 'FULFILLED' && run.winner_provider_id) {
    const winnerProv = run.providers.find(p => p.id === run.winner_provider_id);
    const winnerAttempt = run.attempts.find(a => a.provider_id === run.winner_provider_id);
    const res = winnerAttempt?.result || {};

    winnerContainer.innerHTML = `
      <div class="winner-card">
        <div class="winner-title">
          <span>&#10004;</span> Provider Confirmed: ${winnerProv ? winnerProv.name : run.winner_provider_id}
        </div>
        <div class="spoken-quote">
          "${res.spoken_evidence || 'Confirmed service availability.'}"
        </div>
        <div class="winner-details">
          <div class="winner-detail-item">
            <div class="winner-detail-label">Phone</div>
            <div class="winner-detail-value">${winnerProv ? winnerProv.masked_phone : ''}</div>
          </div>
          <div class="winner-detail-item">
            <div class="winner-detail-label">Quoted Price</div>
            <div class="winner-detail-value">${res.quoted_price_amount ? 'R' + res.quoted_price_amount + ' ' + res.quoted_price_currency : 'Within budget'}</div>
          </div>
          <div class="winner-detail-item">
            <div class="winner-detail-label">ETA</div>
            <div class="winner-detail-value">${res.eta_minutes ? res.eta_minutes + ' min' : (res.delivery_cutoff_spoken || 'Before deadline')}</div>
          </div>
          <div class="winner-detail-item">
            <div class="winner-detail-label">Load-Shedding</div>
            <div class="winner-detail-value">Operating</div>
          </div>
        </div>
      </div>
    `;
    winnerContainer.style.display = 'block';
  } else {
    winnerContainer.innerHTML = '';
    winnerContainer.style.display = 'none';
  }

  // Render Attempts List
  const attemptsList = document.getElementById('attempts-list');
  attemptsList.innerHTML = '';

  run.providers.forEach((provider, index) => {
    const attempt = run.attempts.find(a => a.provider_id === provider.id);
    const card = document.createElement('div');
    const isWinner = run.winner_provider_id === provider.id;
    const isActive = run.status === 'RUNNING' && !attempt?.completed_at && attempt?.started_at;

    card.className = `attempt-card ${isWinner ? 'winner' : ''} ${isActive ? 'active' : ''}`;

    let outcomeText = 'Pending';
    let outcomeBadgeClass = 'badge-cancelled';
    let quoteHtml = '';

    if (attempt && attempt.result) {
      const outcome = attempt.result.outcome;
      outcomeText = outcome;
      if (outcome === 'SUCCESS') outcomeBadgeClass = 'badge-fulfilled';
      else if (outcome === 'REJECTED' || outcome === 'REFUSED') outcomeBadgeClass = 'badge-exhausted';
      else outcomeBadgeClass = 'badge-cancelled';

      if (attempt.result.spoken_evidence) {
        quoteHtml = `<div class="spoken-quote">${attempt.result.spoken_evidence}</div>`;
      }
    } else if (isActive) {
      outcomeText = 'Dialling via CALL-E...';
      outcomeBadgeClass = 'badge-running';
    }

    card.innerHTML = `
      <div class="attempt-header">
        <div>
          <span class="attempt-provider">#${index + 1} ${provider.name}</span>
          <span class="attempt-phone">${provider.masked_phone}</span>
        </div>
        <span class="badge ${outcomeBadgeClass}">${outcomeText}</span>
      </div>
      ${quoteHtml}
    `;

    attemptsList.appendChild(card);
  });
}
