import { useHalone } from '@halone/sdk'

const halone = useHalone()
const out = document.querySelector('#out')!
const show = (value: unknown) => (out.textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2))
const attempt = (action: () => Promise<unknown>) => action().then(show, (error: Error) => show(`${error.name}: ${error.message}`))

halone.windows.self().then(
  (window) => (document.querySelector('#status')!.textContent = `Window ${window.id}, ${window.widthPx}×${window.heightPx}`),
  (error: Error) => (document.querySelector('#status')!.textContent = error.message),
)

let renames = 0
document.querySelector('#title')!.addEventListener('click', () => attempt(() => halone.windows.setTitle(`Hello #${++renames}`)))
document.querySelector('#paste')!.addEventListener('click', () => attempt(() => halone.clipboard.read()))
document.querySelector('#windows')!.addEventListener('click', () => attempt(() => halone.windows.list()))

document.querySelector('#ssh')!.addEventListener('click', () =>
  attempt(async () => {
    const socket = await halone.sockets.connect('github.com', 22)
    const banner = await new Promise<string>((resolve) => socket.onData((bytes) => resolve(new TextDecoder().decode(bytes).trim())))
    await socket.close()
    return banner
  }),
)
