import React, { useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { WebSocketNotificationEvent } from '../services/websocket';
import { Server, Send, Mail, AlertTriangle, Layers, Zap, Smartphone, CheckCircle2 } from 'lucide-react';

interface VisualPipelineProps {
  events: WebSocketNotificationEvent[];
}

export const VisualPipeline: React.FC<VisualPipelineProps> = ({ events }) => {
  // Group events by logId to get the latest status for each message
  const activeMessages = useMemo(() => {
    const map = new Map<string, WebSocketNotificationEvent>();
    events.forEach(evt => {
      map.set(evt.logId, evt);
    });
    return Array.from(map.values());
  }, [events]);

  // Count messages in each node
  const counts = useMemo(() => {
    return {
      API: activeMessages.filter(m => m.status === 'RECEIVED' || m.status === 'QUEUED').length,
      RABBIT: activeMessages.filter(m => m.status === 'QUEUED').length,
      CONSUMER: activeMessages.filter(m => m.status === 'PROCESSING' || m.status === 'RETRYING').length,
      SUCCESS: activeMessages.filter(m => m.status === 'SENT').length,
      DLQ: activeMessages.filter(m => m.status === 'DLQ').length,
    };
  }, [activeMessages]);

  const Node = ({ title, icon: Icon, count, isActive, colorClass }: any) => (
    <div className={`relative flex flex-col items-center justify-center p-4 rounded-xl border-2 w-32 h-32 bg-white transition-all duration-300 ${isActive ? `border-${colorClass}-500 shadow-lg shadow-${colorClass}-100 scale-105` : 'border-slate-200 opacity-80'}`}>
      <div className={`p-3 rounded-full mb-2 ${isActive ? `bg-${colorClass}-100 text-${colorClass}-600` : 'bg-slate-100 text-slate-400'}`}>
        <Icon className="w-8 h-8" />
      </div>
      <span className="font-bold text-slate-700 text-sm tracking-tight">{title}</span>
      <AnimatePresence>
        {count > 0 && (
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            exit={{ scale: 0 }}
            className={`absolute -top-3 -right-3 w-8 h-8 rounded-full flex items-center justify-center font-bold text-white shadow-md bg-${colorClass}-500`}
          >
            {count}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );

  return (
    <div className="flex-1 flex flex-col items-center justify-center w-full h-full p-8 relative">
      
      {/* Background connecting lines */}
      <svg className="absolute inset-0 w-full h-full pointer-events-none" style={{ zIndex: 0 }}>
        {/* API to Rabbit */}
        <line x1="30%" y1="50%" x2="50%" y2="50%" stroke="#e2e8f0" strokeWidth="4" strokeDasharray="8 8" />
        {/* Rabbit to Consumer */}
        <line x1="50%" y1="50%" x2="70%" y2="50%" stroke="#e2e8f0" strokeWidth="4" strokeDasharray="8 8" />
        {/* Consumer to Success */}
        <path d="M 70% 50% Q 85% 50% 85% 30%" fill="none" stroke="#e2e8f0" strokeWidth="4" strokeDasharray="8 8" />
        {/* Consumer to DLQ */}
        <path d="M 70% 50% Q 85% 50% 85% 70%" fill="none" stroke="#e2e8f0" strokeWidth="4" strokeDasharray="8 8" />
      </svg>

      <div className="relative z-10 w-full h-full max-w-5xl flex items-center justify-between">
        
        {/* 1. API Gateway */}
        <div className="flex flex-col items-center" style={{ width: '20%' }}>
          <Node title="API REST" icon={Server} count={counts.API} isActive={counts.API > 0} colorClass="blue" />
        </div>

        {/* 2. RabbitMQ */}
        <div className="flex flex-col items-center" style={{ width: '20%' }}>
          <Node title="RabbitMQ" icon={Layers} count={counts.RABBIT} isActive={counts.RABBIT > 0} colorClass="orange" />
          <span className="mt-2 text-xs font-semibold text-slate-400">notification.queue</span>
        </div>

        {/* 3. Consumer / Strategy */}
        <div className="flex flex-col items-center" style={{ width: '20%' }}>
          <Node title="Consumer" icon={Zap} count={counts.CONSUMER} isActive={counts.CONSUMER > 0} colorClass="purple" />
          <span className="mt-2 text-xs font-semibold text-slate-400">@Retryable</span>
        </div>

        {/* 4. Outcomes (Success vs DLQ) */}
        <div className="flex flex-col justify-between h-full py-10" style={{ width: '20%' }}>
          
          <div className="flex flex-col items-center">
            <Node title="Delivered" icon={CheckCircle2} count={counts.SUCCESS} isActive={counts.SUCCESS > 0} colorClass="green" />
            <span className="mt-2 text-xs font-semibold text-slate-400">AWS SES/SNS</span>
          </div>

          <div className="flex flex-col items-center mt-auto">
            <Node title="DLQ" icon={AlertTriangle} count={counts.DLQ} isActive={counts.DLQ > 0} colorClass="red" />
            <span className="mt-2 text-xs font-semibold text-slate-400">Dead Letter</span>
          </div>

        </div>

      </div>

      {/* Floating particles (animated dots representing messages) */}
      {activeMessages.map(msg => {
        // Determine position based on status
        let position = { left: '30%', top: '50%' };
        let color = 'bg-blue-400';
        
        if (msg.status === 'QUEUED') { position = { left: '50%', top: '50%' }; color = 'bg-orange-400'; }
        if (msg.status === 'PROCESSING') { position = { left: '70%', top: '50%' }; color = 'bg-purple-400'; }
        if (msg.status === 'RETRYING') { position = { left: '70%', top: '50%' }; color = 'bg-yellow-400'; }
        if (msg.status === 'SENT') { position = { left: '85%', top: '25%' }; color = 'bg-green-400'; }
        if (msg.status === 'DLQ') { position = { left: '85%', top: '75%' }; color = 'bg-red-400'; }

        return (
          <motion.div
            key={msg.logId}
            layout
            initial={{ opacity: 0, scale: 0 }}
            animate={{ opacity: 1, scale: 1, left: position.left, top: position.top }}
            transition={{ type: 'spring', stiffness: 100, damping: 15 }}
            className={`absolute w-3 h-3 rounded-full shadow-lg ${color} z-20 -ml-1.5 -mt-1.5`}
            style={{ left: position.left, top: position.top }}
          />
        );
      })}

    </div>
  );
};
