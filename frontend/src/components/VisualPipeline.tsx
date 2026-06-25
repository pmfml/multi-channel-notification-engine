import React, { useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import type { WebSocketNotificationEvent } from '../services/websocket';
import { Server, Send, Mail, AlertTriangle, Layers, Zap, Smartphone, CheckCircle } from 'lucide-react';

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
      API: activeMessages.filter(m => m.eventType === 'RECEIVED' || m.eventType === 'QUEUED').length,
      RABBIT: activeMessages.filter(m => m.eventType === 'QUEUED').length,
      CONSUMER: activeMessages.filter(m => m.eventType === 'PROCESSING' || m.eventType === 'RETRYING').length,
      SUCCESS: activeMessages.filter(m => m.eventType === 'SENT').length,
      DLQ: activeMessages.filter(m => m.eventType === 'DLQ').length,
    };
  }, [activeMessages]);

  const Node = ({ title, icon: Icon, count, isActive, color, left, top, subtitle }: any) => {
    const colors: Record<string, { border: string, iconBg: string, iconText: string, shadow: string, badge: string }> = {
      blue: { border: 'border-blue-500', iconBg: 'bg-blue-100', iconText: 'text-blue-600', shadow: 'shadow-blue-100', badge: 'bg-blue-500' },
      orange: { border: 'border-orange-500', iconBg: 'bg-orange-100', iconText: 'text-orange-600', shadow: 'shadow-orange-100', badge: 'bg-orange-500' },
      purple: { border: 'border-purple-500', iconBg: 'bg-purple-100', iconText: 'text-purple-600', shadow: 'shadow-purple-100', badge: 'bg-purple-500' },
      green: { border: 'border-green-500', iconBg: 'bg-green-100', iconText: 'text-green-600', shadow: 'shadow-green-100', badge: 'bg-green-500' },
      red: { border: 'border-red-500', iconBg: 'bg-red-100', iconText: 'text-red-600', shadow: 'shadow-red-100', badge: 'bg-red-500' },
    };
    
    const theme = colors[color] || colors.blue;

    return (
      <div 
        className="absolute flex flex-col items-center justify-center transform -translate-x-1/2 -translate-y-1/2 z-10"
        style={{ left, top }}
      >
        <div className={`relative flex flex-col items-center justify-center p-4 rounded-xl border-2 w-28 h-28 bg-white transition-all duration-300 ${isActive ? `${theme.border} shadow-lg ${theme.shadow} scale-105` : 'border-slate-200 opacity-80'}`}>
          <div className={`p-3 rounded-full mb-2 ${isActive ? `${theme.iconBg} ${theme.iconText}` : 'bg-slate-100 text-slate-400'}`}>
            <Icon className="w-8 h-8" />
          </div>
          <span className="font-bold text-slate-700 text-sm tracking-tight whitespace-nowrap">{title}</span>
          <AnimatePresence>
            {count > 0 && (
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                exit={{ scale: 0 }}
                className={`absolute -top-3 -right-3 w-8 h-8 rounded-full flex items-center justify-center font-bold text-white shadow-md ${theme.badge}`}
              >
                {count}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
        {subtitle && <span className="mt-2 text-xs font-semibold text-slate-400">{subtitle}</span>}
      </div>
    );
  };

  return (
    <div className="flex-1 flex items-center justify-center w-full h-full p-8 relative overflow-hidden bg-slate-50/50">
      
      {/* Container for absolute positioning */}
      <div className="relative w-full h-[400px] max-w-4xl">
        
        {/* Background connecting lines */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none" style={{ zIndex: 0 }}>
          {/* API to Rabbit */}
          <line x1="15%" y1="50%" x2="40%" y2="50%" stroke="#cbd5e1" strokeWidth="3" strokeDasharray="6 6" />
          {/* Rabbit to Consumer */}
          <line x1="40%" y1="50%" x2="65%" y2="50%" stroke="#cbd5e1" strokeWidth="3" strokeDasharray="6 6" />
          {/* Consumer to Success (Elbow L-curve built with 3 lines) */}
          <line x1="65%" y1="50%" x2="75%" y2="50%" stroke="#cbd5e1" strokeWidth="3" strokeDasharray="6 6" />
          <line x1="75%" y1="50%" x2="75%" y2="25%" stroke="#cbd5e1" strokeWidth="3" strokeDasharray="6 6" />
          <line x1="75%" y1="25%" x2="90%" y2="25%" stroke="#cbd5e1" strokeWidth="3" strokeDasharray="6 6" />
          
          {/* Consumer to DLQ (Elbow L-curve built with 2 lines sharing the first horizontal segment) */}
          <line x1="75%" y1="50%" x2="75%" y2="75%" stroke="#cbd5e1" strokeWidth="3" strokeDasharray="6 6" />
          <line x1="75%" y1="75%" x2="90%" y2="75%" stroke="#cbd5e1" strokeWidth="3" strokeDasharray="6 6" />
        </svg>

        {/* The 5 Nodes placed precisely over the SVG lines */}
        <Node title="API REST" icon={Server} count={counts.API} isActive={counts.API > 0} color="blue" left="15%" top="50%" />
        <Node title="RabbitMQ" icon={Layers} count={counts.RABBIT} isActive={counts.RABBIT > 0} color="orange" left="40%" top="50%" subtitle="notification.queue" />
        <Node title="Consumer" icon={Zap} count={counts.CONSUMER} isActive={counts.CONSUMER > 0} color="purple" left="65%" top="50%" subtitle="@Retryable" />
        <Node title="Delivered" icon={CheckCircle} count={counts.SUCCESS} isActive={counts.SUCCESS > 0} color="green" left="90%" top="25%" subtitle="AWS SES/SNS" />
        <Node title="DLQ" icon={AlertTriangle} count={counts.DLQ} isActive={counts.DLQ > 0} color="red" left="90%" top="75%" subtitle="Dead Letter" />

        {/* Floating particles (animated dots representing messages) */}
        {activeMessages.map(msg => {
          // Exact coordinates matching the Nodes
          let position = { left: '15%', top: '50%' };
          let color = 'bg-blue-500';
          
          if (msg.eventType === 'QUEUED') { position = { left: '40%', top: '50%' }; color = 'bg-orange-500'; }
          if (msg.eventType === 'PROCESSING') { position = { left: '65%', top: '50%' }; color = 'bg-purple-500'; }
          if (msg.eventType === 'RETRYING') { position = { left: '65%', top: '50%' }; color = 'bg-yellow-500'; }
          if (msg.eventType === 'SENT') { position = { left: '90%', top: '25%' }; color = 'bg-green-500'; }
          if (msg.eventType === 'DLQ') { position = { left: '90%', top: '75%' }; color = 'bg-red-500'; }

          return (
            <motion.div
              key={msg.logId}
              layout
              initial={{ opacity: 0, scale: 0 }}
              animate={{ opacity: 1, scale: 1, left: position.left, top: position.top }}
              transition={{ type: 'tween', ease: 'easeInOut', duration: 1.0 }}
              className={`absolute w-5 h-5 rounded-full shadow-md ${color} z-20`}
              style={{ x: '-50%', y: '-50%' }}
            />
          );
        })}
      </div>

    </div>
  );
};
