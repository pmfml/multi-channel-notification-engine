import { useEffect, useState } from 'react'
import { MainLayout } from './components/layout/MainLayout'
import { TerminalLog } from './components/TerminalLog'
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
      {/* Top half: The Visual Pipeline (To be built in Step 7) */}
      <div className="flex-1 mb-6 flex flex-col">
        <h2 className="text-xl font-bold text-slate-800 mb-4 tracking-tight">Real-Time Event Pipeline</h2>
        <div className="flex-1 bg-white border border-slate-200 shadow-sm rounded-xl flex items-center justify-center p-8">
          <p className="text-slate-400 text-lg font-medium">(O Caminho Desenhado do Fluxo será implementado no Step 7)</p>
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
