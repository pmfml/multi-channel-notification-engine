import { useEffect, useState } from 'react'
import { MainLayout } from './components/layout/MainLayout'
import { TerminalLog } from './components/TerminalLog'
import { VisualPipeline } from './components/VisualPipeline'
import { wsService, WebSocketNotificationEvent } from './services/websocket'

function App() {
  const [events, setEvents] = useState<WebSocketNotificationEvent[]>([])

  useEffect(() => {
    // Connect to WebSockets on mount
    wsService.connect((newEvent) => {
      setEvents((prev) => [...prev, newEvent])
    })

    return () => {
      wsService.disconnect()
    }
  }, [])

  return (
    <MainLayout>
      {/* Top half: The Visual Pipeline */}
      <div className="flex-1 mb-6 flex flex-col min-h-[400px]">
        <h2 className="text-xl font-bold text-slate-800 mb-4 tracking-tight">Real-Time Event Pipeline</h2>
        <div className="flex-1 bg-white border border-slate-200 shadow-sm rounded-xl flex items-center justify-center overflow-hidden">
          <VisualPipeline events={events} />
        </div>
      </div>

      {/* Bottom half: Terminal Logs */}
      <div className="shrink-0">
        <TerminalLog events={events} />
      </div>
    </MainLayout>
  )
}

export default App
