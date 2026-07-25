let conversationId = null;
const statusNode = document.getElementById('status');
const modelNode = document.getElementById('model');
const systemNode = document.getElementById('system-info');
const messagesNode = document.getElementById('messages');
const form = document.getElementById('chat-form');
const input = document.getElementById('message');
const button = form.querySelector('button');

function addMessage(role, content) {
  const node = document.createElement('div');
  node.className = `message ${role}`;
  node.textContent = content;
  messagesNode.appendChild(node);
  messagesNode.scrollTop = messagesNode.scrollHeight;
}

async function loadStatus() {
  try {
    const [healthResponse, systemResponse] = await Promise.all([
      fetch('/health'),
      fetch('/api/system')
    ]);
    if (!healthResponse.ok || !systemResponse.ok) throw new Error('Server unavailable');
    const health = await healthResponse.json();
    const system = await systemResponse.json();
    statusNode.textContent = 'Local server online';
    statusNode.className = 'status ok';
    modelNode.textContent = health.model.model;
    const rows = [
      ['Device', system.device_model || 'Unknown'],
      ['Android', system.android_version || 'Not detected'],
      ['Architecture', system.architecture],
      ['RAM', `${system.ram_total_gb} GB`],
      ['Free storage', `${system.storage_free_gb} GB`],
      ['Compatibility', system.compatibility_tier]
    ];
    systemNode.innerHTML = rows.map(([key, value]) => `<div><dt>${key}</dt><dd>${value}</dd></div>`).join('');
  } catch (error) {
    statusNode.textContent = 'Server unavailable';
    statusNode.className = 'status error';
  }
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const message = input.value.trim();
  if (!message) return;
  addMessage('user', message);
  input.value = '';
  button.disabled = true;
  try {
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({message, conversation_id: conversationId})
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.detail || 'Generation failed');
    conversationId = payload.conversation_id;
    addMessage('assistant', payload.response);
  } catch (error) {
    addMessage('assistant', `Error: ${error.message}`);
  } finally {
    button.disabled = false;
    input.focus();
  }
});

loadStatus();
