import { Fragment, useState } from 'react';
import { codingHelpAPI } from '../api';

const starters = [
  { icon: '↻', title: 'Understand recursion', prompt: 'Explain recursion with a simple Java example.' },
  { icon: '⌁', title: 'Debug an error', prompt: 'How should I debug a null pointer error?' },
  { icon: '⇄', title: 'Compare concepts', prompt: 'What is the difference between a stack and a queue?' },
];

const WELCOME = { role: 'assistant', text: 'Hi! I’m your coding study partner. Ask about concepts, errors, or how to approach a problem, and I’ll help you think it through.' };

export default function CodingHelpPage() {
  const [messages, setMessages] = useState([WELCOME]);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);

  const send = async (value = draft) => {
    const text = value.trim();
    if (!text || sending) return;
    setMessages((items) => [...items, { role: 'user', text }]);
    setDraft('');
    setSending(true);
    try {
      const { data } = await codingHelpAPI.chat(text);
      setMessages((items) => [...items, { role: 'assistant', text: data.answer }]);
    } catch (error) {
      setMessages((items) => [...items, { role: 'assistant', error: true, text: error.response?.data?.message || 'Coding Help is unavailable right now.' }]);
    } finally { setSending(false); }
  };

  return <div className="container fade-in coding-help-page">
    <section className="coding-help-hero">
      <div className="coding-help-orb">✦</div>
      <div><div className="coding-help-eyebrow"><span /> AI study partner</div><h1>Get unstuck. Keep learning.</h1><p>Talk through code, concepts, and debugging without leaving CodeSphere.</p></div>
      <button className="btn btn-ghost btn-sm coding-help-clear" onClick={() => setMessages([WELCOME])} disabled={messages.length === 1 || sending}>Clear chat</button>
    </section>

    <div className="coding-help-layout">
      <aside className="coding-help-sidebar">
        <div className="coding-help-sidebar-title">Try asking about</div>
        {starters.map((starter) => <button key={starter.title} className="coding-help-starter" onClick={() => send(starter.prompt)} disabled={sending}><span>{starter.icon}</span><div><strong>{starter.title}</strong><small>{starter.prompt}</small></div></button>)}
        <div className="coding-help-note"><span>⌘</span><p><strong>Learning first</strong>Get explanations and hints, not live assessment answers.</p></div>
      </aside>

      <section className="coding-help-chat">
        <header className="coding-help-chat-header"><div className="coding-help-avatar">✦</div><div><strong>CodeSphere AI</strong><small><i /> Gemini · ready to help</small></div></header>
        <div className="coding-help-messages">
          {messages.map((message, index) => <div className={`coding-help-message ${message.role} ${message.error ? 'error' : ''}`} key={index}><div className="coding-help-message-label">{message.role === 'user' ? 'You' : 'CodeSphere AI'}</div><MarkdownText text={message.text} /></div>)}
          {sending && <div className="coding-help-message assistant"><div className="coding-help-message-label">CodeSphere AI</div><div className="coding-help-typing"><span /><span /><span /></div></div>}
        </div>
        <footer className="coding-help-compose"><textarea className="textarea" value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); send(); } }} placeholder="Ask a coding question…" disabled={sending} /><div><small>Press Enter to send · Shift + Enter for a new line</small><button className="btn btn-primary" onClick={() => send()} disabled={sending || !draft.trim()}>Send <span>↑</span></button></div></footer>
      </section>
    </div>
  </div>;
}

function MarkdownText({ text }) {
  const lines = String(text || '').split('\n');
  const blocks = [];
  let listItems = [];
  let codeLines = [];
  let inCode = false;
  const flushList = () => { if (listItems.length) { blocks.push(<ul key={`list-${blocks.length}`}>{listItems.map((item, index) => <li key={index}>{inline(item)}</li>)}</ul>); listItems = []; } };
  const flushCode = () => { if (codeLines.length) { blocks.push(<pre key={`code-${blocks.length}`}><code>{codeLines.join('\n')}</code></pre>); codeLines = []; } };

  lines.forEach((line) => {
    if (line.trim().startsWith('```')) { if (inCode) flushCode(); else flushList(); inCode = !inCode; return; }
    if (inCode) { codeLines.push(line); return; }
    const bullet = line.match(/^\s*[-*]\s+(.+)/);
    if (bullet) { listItems.push(bullet[1]); return; }
    flushList();
    if (!line.trim()) { blocks.push(<div className="coding-help-spacer" key={`space-${blocks.length}`} />); return; }
    blocks.push(<p key={`text-${blocks.length}`}>{inline(line)}</p>);
  });
  flushList();
  if (inCode) flushCode();
  return <div className="coding-help-markdown">{blocks}</div>;
}

function inline(value) {
  return String(value).split(/(\*\*[^*]+\*\*|`[^`]+`)/g).filter(Boolean).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={index}>{part.slice(2, -2)}</strong>;
    if (part.startsWith('`') && part.endsWith('`')) return <code key={index}>{part.slice(1, -1)}</code>;
    return <Fragment key={index}>{part}</Fragment>;
  });
}
