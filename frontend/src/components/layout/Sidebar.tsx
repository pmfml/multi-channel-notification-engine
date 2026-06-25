import React from 'react';
import { Layers } from 'lucide-react';
import { MessageControls } from '../MessageControls';

export const Sidebar: React.FC = () => {
  return (
    <div className="w-80 bg-white border-r border-slate-200 h-screen flex flex-col shadow-sm relative z-10">
      {/* Header */}
      <div className="p-6 border-b border-slate-100">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-200">
            <Layers className="text-white w-6 h-6" />
          </div>
          <div>
            <h1 className="font-bold text-slate-800 text-lg leading-tight">MCNE</h1>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Visualizer</p>
          </div>
        </div>
      </div>

      {/* Controls Area */}
      <div className="p-6 flex-1 overflow-y-auto custom-scrollbar">
        <MessageControls />
      </div>
      
      {/* Footer / Status */}
      <div className="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
        <div className="flex items-center gap-2">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
          </span>
          WS Connected
        </div>
        <span>v1.0.0</span>
      </div>
    </div>
  );
};
