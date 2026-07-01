import React, { useState } from 'react';
import { Send, Zap, RotateCcw, AlertTriangle, Settings } from 'lucide-react';
import { mcneApi } from '../services/api';

export const MessageControls: React.FC = () => {
  const [loading, setLoading] = useState(false);

  // Single Message State
  const [recipient, setRecipient] = useState('user@example.com');
  const [channel, setChannel] = useState<'EMAIL' | 'SMS'>('EMAIL');
  const [simulateError, setSimulateError] = useState(false);

  // Batch State
  const [batchCount, setBatchCount] = useState(20);
  const [batchErrorRate, setBatchErrorRate] = useState(10); // 10% errors

  // Settings State
  const [concurrency, setConcurrency] = useState(1);

  const handleSendSingle = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const metadata: Record<string, string> = { demoDelayMs: '750' };
      if (simulateError) metadata['simulateError'] = 'true';

      await mcneApi.sendNotification({
        recipient,
        message: 'Hello from Visualizer!',
        channel,
        metadata
      });
    } catch (err) {
      console.error('Failed to send:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSendBatch = async () => {
    setLoading(true);
    try {
      const promises = [];
      for (let i = 0; i < batchCount; i++) {
        const isError = Math.random() * 100 < batchErrorRate;
        const randChannel = Math.random() > 0.5 ? 'EMAIL' : 'SMS';
        const metadata: Record<string, string> = { demoDelayMs: '750' };
        if (isError) metadata['simulateError'] = 'true';

        promises.push(
          mcneApi.sendNotification({
            recipient: `batch-user-${i}@example.com`,
            message: `Batch message #${i}`,
            channel: randChannel,
            metadata
          })
        );
      }
      // Fire all concurrently
      await Promise.all(promises);
    } catch (err) {
      console.error('Batch failed:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReprocessDlq = async () => {
    setLoading(true);
    try {
      await mcneApi.reprocessDlq();
    } catch (err) {
      console.error('Reprocess DLQ failed:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col gap-5">
      {/* SINGLE MESSAGE FORM */}
      <section>
        <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
          <Send className="w-4 h-4" /> Single Message
        </h2>
        <form onSubmit={handleSendSingle} className="flex flex-col gap-2">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Channel</label>
            <select
              value={channel}
              onChange={e => setChannel(e.target.value as 'EMAIL' | 'SMS')}
              className="w-full bg-slate-50 border border-slate-200 text-slate-800 text-sm rounded-lg p-2.5 focus:ring-blue-500 focus:border-blue-500 outline-none"
            >
              <option value="EMAIL">EMAIL</option>
              <option value="SMS">SMS</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Recipient</label>
            <input
              type="text"
              value={recipient}
              onChange={e => setRecipient(e.target.value)}
              className="w-full bg-slate-50 border border-slate-200 text-slate-800 text-sm rounded-lg p-2.5 outline-none"
            />
          </div>
          <div className="flex items-center mt-1">
            <input
              type="checkbox"
              id="simError"
              checked={simulateError}
              onChange={e => setSimulateError(e.target.checked)}
              className="w-4 h-4 text-red-600 bg-gray-100 border-gray-300 rounded focus:ring-red-500"
            />
            <label htmlFor="simError" className="ml-2 text-xs font-medium text-slate-600">
              Force AWS Error (Simulate Retry/DLQ)
            </label>
          </div>
          <button
            type="submit"
            disabled={loading}
            className="mt-2 w-full text-white bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:ring-blue-300 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none disabled:opacity-50 transition-colors"
          >
            Send Message
          </button>
        </form>
      </section>

      {/* BATCH SIMULATOR */}
      <section>
        <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
          <Zap className="w-4 h-4" /> Batch Simulator
        </h2>
        <div className="flex flex-col gap-2 bg-slate-50 p-3 rounded-xl border border-slate-200">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Payload Size: <span className="font-bold text-blue-600">{batchCount} msgs</span></label>
            <input
              type="range"
              min="1" max="100"
              value={batchCount}
              onChange={e => setBatchCount(parseInt(e.target.value))}
              className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Failure Injection: <span className="font-bold text-red-500">{batchErrorRate}%</span></label>
            <input
              type="range"
              min="0" max="100"
              value={batchErrorRate}
              onChange={e => setBatchErrorRate(parseInt(e.target.value))}
              className="w-full h-2 bg-red-200 rounded-lg appearance-none cursor-pointer accent-red-600"
            />
          </div>
          <button
            onClick={handleSendBatch}
            disabled={loading}
            className="mt-3 w-full text-slate-800 bg-white border border-slate-300 hover:bg-slate-100 focus:ring-4 focus:ring-slate-100 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none disabled:opacity-50 transition-colors shadow-sm"
          >
            Fire Async Batch
          </button>
        </div>
      </section>

      {/* OPERATIONS PANEL */}
      <section>
        <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
          <Settings className="w-4 h-4" /> Infrastructure Settings
        </h2>
        <div className="flex flex-col gap-2 bg-slate-50 p-3 rounded-xl border border-slate-200">
          <div>
            <div className="flex justify-between mb-1">
              <label className="text-sm font-medium text-slate-700">Consumer Threads</label>
              <span className="text-sm font-bold text-slate-900">
                {concurrency === 0 ? '0 (Stopped)' : concurrency}
              </span>
            </div>
            <input
              type="range"
              min="0"
              max="10"
              value={concurrency}
              onChange={(e) => {
                const val = parseInt(e.target.value);
                setConcurrency(val);
                mcneApi.setConcurrency(val).catch(err => console.error(err));
              }}
              className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
            />
            <p className="text-xs text-slate-500 mt-1">
              0 stops the consumer. 1+ processes messages concurrently.
            </p>
          </div>
        </div>
      </section>

      {/* DLQ ACTIONS */}
      <section>
        <h2 className="text-xs font-bold text-red-400 uppercase tracking-wider mb-4 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4" /> Dead Letter Queue
        </h2>
        <button
          onClick={handleReprocessDlq}
          disabled={loading}
          className="w-full text-white bg-slate-800 hover:bg-slate-900 focus:ring-4 focus:ring-slate-300 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none disabled:opacity-50 transition-colors shadow-sm flex items-center justify-center gap-2"
        >
          <RotateCcw className="w-4 h-4" /> Reprocess All DLQ
        </button>
      </section>
    </div>
  );
};
