import React, { useEffect, useRef } from 'react';
import { WebSocketNotificationEvent } from '../services/websocket';
import { Terminal, Clock, Activity, CheckCircle2, XCircle, AlertTriangle } from 'lucide-react';

interface TerminalLogProps {
  events: WebSocketNotificationEvent[];
}

export const TerminalLog: React.FC<TerminalLogProps> = ({ events }) => {
  const bottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new events arrive
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events]);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'QUEUED': return <Clock className="w-4 h-4 text-blue-400" />;
      case 'PROCESSING': return <Activity className="w-4 h-4 text-purple-400" />;
      case 'SENT': return <CheckCircle2 className="w-4 h-4 text-green-400" />;
      case 'RETRYING': return <AlertTriangle className="w-4 h-4 text-yellow-400" />;
      case 'DLQ': return <XCircle className="w-4 h-4 text-red-400" />;
      default: return <Terminal className="w-4 h-4 text-gray-400" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'QUEUED': return 'text-blue-400';
      case 'PROCESSING': return 'text-purple-400';
      case 'SENT': return 'text-green-400';
      case 'RETRYING': return 'text-yellow-400';
      case 'DLQ': return 'text-red-400';
      default: return 'text-gray-400';
    }
  };

  return (
    <div className="flex flex-col bg-slate-950 text-slate-300 rounded-lg overflow-hidden border border-slate-800 shadow-xl h-64">
      <div className="flex items-center px-4 py-2 bg-slate-900 border-b border-slate-800">
        <Terminal className="w-4 h-4 mr-2 text-slate-400" />
        <span className="text-sm font-semibold tracking-wide text-slate-200">System Logs</span>
      </div>
      <div className="flex-1 p-4 overflow-y-auto font-mono text-sm leading-relaxed">
        {events.length === 0 ? (
          <div className="flex items-center justify-center h-full text-slate-600">
            Waiting for events...
          </div>
        ) : (
          <div className="space-y-2">
            {events.map((evt, idx) => (
              <div key={idx} className="flex items-start space-x-3">
                <span className="mt-0.5">{getStatusIcon(evt.status)}</span>
                <span className="text-slate-500 shrink-0">[{new Date().toLocaleTimeString()}]</span>
                <span className={`font-bold shrink-0 ${getStatusColor(evt.status)}`}>
                  [{evt.status}]
                </span>
                <span className="text-blue-300 shrink-0">[{evt.channel}]</span>
                <span className="text-slate-400 break-words line-clamp-1">{evt.message}</span>
                <span className="text-slate-600 shrink-0 text-xs ml-auto">ID: {evt.logId.split('-')[0]}</span>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>
        )}
      </div>
    </div>
  );
};
