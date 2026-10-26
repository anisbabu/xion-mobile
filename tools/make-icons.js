// Renders the Xion Mobile launcher icons into every mipmap density.
// Run from D:\XionLauncher (for its Electron): npx electron D:/XionMobile/tools/make-icons.js
const { app, BrowserWindow } = require('electron')
const fs = require('fs')
const path = require('path')

const RES = 'D:/XionMobile/app_pojavlauncher/src/main/res'
const DENS = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 }
const logo = fs.readFileSync('D:/XionLauncher/build/logo.svg', 'utf8')

// The X mark alone, centred in the adaptive-icon safe zone (66 of 108 units).
const MARK = `<g transform="translate(25.5 25.5) scale(0.4453)">
  <defs>
    <linearGradient id="a" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffffff"/><stop offset="1" stop-color="#b4c0d6"/></linearGradient>
    <linearGradient id="b" x1="1" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#e3eaf7"/><stop offset="1" stop-color="#7f8ca6"/></linearGradient>
  </defs>
  <circle cx="64" cy="21" r="4.5" fill="#e9eef7" opacity=".9"/>
  <path d="M30 30h19l49 68H79z" fill="url(#a)"/>
  <path d="M79 30h19L74 63 64.5 50z" fill="url(#b)"/>
  <path d="M54 76 63.5 89 57 98H30z" fill="url(#b)"/>
</g>`
const FOREGROUND = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">${MARK}</svg>`
const BACKGROUND = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
  <defs><linearGradient id="t" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1b2130"/><stop offset="1" stop-color="#090b10"/></linearGradient>
  <linearGradient id="p" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#4a5670"/><stop offset="1" stop-color="#1b2130" stop-opacity="0"/></linearGradient></defs>
  <rect width="108" height="108" fill="url(#t)"/>
  <path d="M0 84 26 60l12 10 21-26 17 19 32-24v69H0z" fill="url(#p)" opacity=".8"/>
</svg>`
const ROUND = logo.replace('rx="30"', 'rx="60"').replace('rx="29.25"', 'rx="59.25"')
  .replace('</defs>', '<clipPath id="rc"><circle cx="64" cy="64" r="59.5"/></clipPath></defs>').replace('<path d="M4 94', '<path clip-path="url(#rc)" d="M4 94')

async function render (win, svg, size, file) {
  const html = `<html><body style="margin:0;background:transparent;overflow:hidden">${svg.replace('<svg ', `<svg width="${size}" height="${size}" `)}</body></html>`
  await win.loadURL('data:text/html,' + encodeURIComponent(html))
  await new Promise(r => setTimeout(r, 150))
  const img = await win.webContents.capturePage({ x: 0, y: 0, width: size, height: size })
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, img.resize({ width: size, height: size }).toPNG())
}

app.disableHardwareAcceleration()
app.on('window-all-closed', () => {})
app.whenReady().then(async () => {
  const win = new BrowserWindow({ width: 512, height: 512, show: false, frame: false, transparent: true, webPreferences: { offscreen: true } })
  for (const [d, k] of Object.entries(DENS)) {
    const dir = `${RES}/mipmap-${d}`
    for (const f of fs.readdirSync(dir)) if (f.endsWith('.webp')) fs.unlinkSync(path.join(dir, f))
    await render(win, logo, Math.round(48 * k), `${dir}/ic_launcher.png`)
    await render(win, ROUND, Math.round(48 * k), `${dir}/ic_launcher_round.png`)
    await render(win, FOREGROUND, Math.round(108 * k), `${dir}/ic_launcher_foreground.png`)
    await render(win, BACKGROUND, Math.round(108 * k), `${dir}/ic_launcher_background.png`)
  }
  console.log('icons written')
  app.exit(0)
})
