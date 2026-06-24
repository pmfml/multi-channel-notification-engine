import React from 'react';
import { Send, Zap, Layers } from 'lucide-react';

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

      {/* Controls Placeholder */}
      <div className="p-6 flex-1 overflow-y-auto">
        <h2 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Message Controls</h2>
        
        <div className="space-y-4">
          <div className="p-4 border border-dashed border-slate-300 rounded-lg bg-slate-50 text-slate-500 text-sm text-center">
            (Formulário de Envio Único virá aqui no Step 8)
          </div>
        </div>

        <h2 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mt-8 mb-4">Batch Simulation</h2>
        <div className="space-y-4">
          <div className="p-4 border border-dashed border-slate-300 rounded-lg bg-slate-50 text-slate-500 text-sm text-center">
            (Controles de Concorrência virão aqui no Step 8)
          </div>
        </div>
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
