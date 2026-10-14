# A touch terminal for Arbace's REPL: a survey and plan

Status: survey and plan for discussion, 2026-10-14, long term (after `arbace-inside-itself-v1`;
the agenda's Later). Nothing here is built. The proposals (IR1-IR13, §8) are the user's to
decide. The user's screenshots of the model app (§2); §3 lists what more would answer.

## Why

The user reaches the host from an iPhone through Moshi, a mosh terminal for iOS, over a mobile
network. The goal of the standalone Arbace is a self-sustaining REPL in a sandbox, in Arbace
down to the ISA; its interface today is a cooked console under rlwrap (go/EXEC-NOTES.md, "Line
editing: rlwrap"; go/BOX-NOTES.md, S1). A touch interface of Arbace's own, as good as Moshi's on
a phone, is the REPL's first interface beyond a terminal. The user named Moshi's tooling as one
to study (AGENDA, Later): a Go backend on Linux and macOS and a GPU-accelerated Go frontend on
macOS, confirmed by the user's measurements; the iOS app's tooling is not known.

## 1. Clean room

The user's definition (2026-10-14): clean room means **no reverse engineering** of the model app
(no inspection of its binaries or resources, no decompiling, no interception of its traffic to
others' servers). Source code that is published, mosh's for one, may be studied and used, under
its license as `LICENSE.md` records it.

- **The model app** is known only as a user knows it: its screens, its behavior, its public
  pages, and what it sends to a server we run (our own `mosh-server` and `sshd`). The
  specification (§4) is written from that, with no artwork, icons, themes' colors or wording
  taken; the product's name stays out of Arbace's UI and code (this document names it as the
  model). The bundle inspection suggested earlier on 2026-10-14 (Apple Configurator, `otool -L`)
  is reverse engineering by this definition and is dropped.
- **mosh** (github.com/mobile-shell/mosh; GPL-3.0 or later, with an exception for OpenSSL) may
  be studied: its source, its protocol as the source defines it, and the paper (Winstein and
  Balakrishnan, "Mosh: An Interactive Remote Shell for Mobile Clients", USENIX ATC 2012). What
  is taken is recorded as for any prior art (the journal, `doc/ARBACE.md` or the vendored
  code's notes, `LICENSE.md`). The license matters for how it is taken (IR6): Arbace is EPL 1.0
  (`LICENSE.md`), and the EPL 1.0 and the GPL do not combine in one program, so translating or
  vendoring mosh's code would make the client a GPL work; a client of Arbace's own written with
  mosh's source as the reference for the protocol keeps it EPL.
- **Other sources**: the escape sequences from ECMA-48, DEC's VT100/VT220 manuals and xterm's
  "ctlseqs"; open terminal emulators (xterm, SwiftTerm, libvterm, alacritty, ...) may be studied
  under their licenses in the same way; the font (§2: the model uses JetBrains Mono, OFL 1.1)
  can be vendored with its license.

## 2. What is observed (screenshots, 2026-10-14)

**The terminal** (a session on the host, `free`):

- A full-screen dark terminal on a sheet with a grab handle; above the text, a row with two round
  buttons (a yellow one with a minus: back to the home screen, the session kept; a green one
  with a list: the sessions), the connection's name, and on the right a badge naming the
  transport ("Mosh").
- A monospace font (JetBrains Mono, per its settings) with a slashed zero, ANSI colors in the
  prompt, a block cursor (hollow when the keyboard is hidden), about 46 columns in portrait.
- Above the system keyboard, a rounded toolbar, scrollable and configurable (Settings, Input,
  Toolbar): **Ctrl** (a latch: it lights up green until the next key), **Esc**, **Tab**, a **pad**
  key, a **list** key, **paste**, a curved arrow, a **chat** key, and a key that hides or shows the
  keyboard.
- **The pad**: a floating cross of keys over the terminal, with a drag handle and a lock (to pin
  it in place): up, down, left, right, Enter in the middle, backspace and a **clear** key (a broom)
  in the top corners. With the keyboard hidden, the terminal takes the screen and the pad sends
  the keys.
- **The chat composer** (the chat key): a text field at the bottom ("Chat via ..."), with buttons
  to attach (+), open the toolbar, close (×), dictate (mic) and send (↑). It composes a whole
  message before sending it to the session (for an agent running in the terminal, or any long
  line), with dictation, instead of typing into the terminal key by key.
- **Selection**: a long press selects a word on the grid with two handles; the menu offers
  **Copy** and **Comment** (a comment on the selected output, presumably attached to a chat
  message).
- The system keyboard (here the Hungarian QWERTZ layout, with the globe and dictation keys): the
  app takes system input.

**The home screen**:

- **Sessions**: live thumbnails of each open session's screen with its name; long press for
  options.
- **Connections**: saved hosts (a name, `user@host:port`, a status dot); long press for options;
  a floating **+** to add one.
- Two round buttons at the top: a ring (a gauge; what it measures is not shown) and Settings.
- A "Discover" row of feature cards (Chat View, Sidebar, Inbox & Approvals; 18 of 18 seen).

**A new connection** (a sheet with two ways):

- **Pair with a QR code**: run one command on the Mac or Linux machine, scan the QR code it
  prints, and the app installs its SSH key there (about a minute).
- **Your own SSH**: host name, user and key by hand; mosh keeps the session across networks.

**Settings**: a paid tier and its license; Terminal (theme, font and size, advanced); Agents
(hooks, notifications, chat view, sidebar); Input (toolbar, shortcuts, more below the screen).

From the host's side: the session's server is the stock `mosh-server` (`new -s -c 256`), reached
after an SSH login, so the app is an SSH and mosh client, 256 colors.

**The Agents settings** show what the app is built around: coding agents running in the terminal
(hooks the agent calls, notifications to the phone, an inbox where its requests are approved).
That is the model's server side on Linux and macOS (Go, per the user's measurements). For
Arbace, the same shape fits the REPL: a long evaluation or a future that finishes, a question
the REPL asks, as notifications and approvals.

**A screen recording** (72 s, 2026-10-14, the user's; frames every 3 s):

- **Reconnecting**: on returning to the session the header reads "Reconnecting..." in place of
  the connection's name; the screen keeps the last state until the server's arrives (mosh's
  state synchronization), then the session continues.
- **The toolbar scrolls** sideways: past the paste key come a jump key (with nothing to jump to it
  says so in a toast at the top, "Nothing to jump to": presumably to the previous prompt or
  output mark), a history key (a clock with an arrow), the chat key and the keyboard key.
- **The composer's own toolbar**: close, Esc, the pad, history and a stack (shortcuts or saved
  snippets), beside the send button.
- **The home screen's banner**: a count of connections this month against the free tier.
- **The ring** at the top opens a **Usage** sheet: an agent account's usage limits (a five-hour
  and a seven-day window as bars, with the time to reset), read from the agent running on the
  host. The model integrates with Claude Code on the host through a hook command the user
  installed there (`~/.claude/settings.json` lists it for Notification, PermissionRequest and
  PostToolUse; not inspected further: its binary is the model's).
- **The inbox** (a house icon beside settings): a "Done" list of the agent's finished turns, each
  with its first words, the prompt, the project directory, the model and the age (here a turn of
  this very conversation), and a live pill of the agent's progress at the bottom.
- **More settings**: Input (toolbar, shortcuts, gestures, speech, hardware keyboard); Security
  and sync (biometrics for keys and on resume, SSH keys, sync of settings and of credentials);
  General (language, app icon, what the home screen shows); Help (docs, what's new, support, open
  source licenses); version 4.0.1.

## 3. What more screenshots would answer

1. The jump and history keys at work (with output to jump through, with a history).
2. What Comment does with a selection; the composer's stack button.
3. Scrollback (swipe), pinch to zoom, landscape, an external keyboard.
4. A long loss of network (the recording shows a short reconnect).
5. Settings' Advanced, Toolbar, Shortcuts, Gestures, Speech, Hooks, Notifications, Sidebar.
6. The QR pairing's command on the host (what it installs and runs).
7. Help, "Open Source Licenses": the libraries the app ships, as the app itself shows them to
   any user; it answers which tooling the iOS app uses without any reverse engineering.

## 4. The functional specification to write (the specification side)

A document of behaviors, numbered for tests: the screen's regions; every key of the accessory
bar and its effect in bytes; gestures; the session's lifecycle (connect, roam, suspend in the
background, resume, end); what is shown in each connection state; settings. Each behavior gets
a check that a build can pass (§7).

## 5. Architecture

### 5.1 Where the REPL runs

- **Remote**: Arbace runs on the host (natively, or in the box) and the phone is a terminal to
  it, as Moshi is today. Everything already exists on the server side; the phone needs a
  terminal and a transport.
- **On the device**: Arbace runs on the iPhone. iOS gives third parties no hypervisor, so not
  in the box: the native program, built for `ios/arm64` (Go supports it), sandboxed by iOS
  itself. Arbace's evaluator interprets and its code is compiled ahead of time (S6), so no JIT,
  which the App Store forbids; an interpreter is allowed. The program is large (75 MB AOT,
  S6) and uses gigabytes for a2g, but the REPL alone fits a phone's limits (to measure).
- Both: one interface over two transports, local (a pipe to the in-process REPL) and remote.

### 5.2 Layers

1. **The terminal core**, platform-free: the escape-sequence parser (a state machine after
   Paul Williams' published VT500 parser diagram), the screen grid with attributes, scrollback,
   the alternate screen, wide and combining characters (Unicode widths), the modes the REPL and a
   shell need. Arbace (class forms or Clojure), so it runs and is tested headless on the host.
2. **The transport**: mosh's protocol (UDP, AES-128-OCB, its state-synchronization over
   datagrams, the client's predictive local echo), or SSH, or a REPL protocol of Arbace's own
   (§5.4). Also Arbace.
3. **The renderer**: glyphs into an atlas, the grid drawn as quads by Metal (the GPU path the
   model's macOS frontend takes), or by Core Text and Core Graphics first (simpler, slower).
4. **The platform shell**: the view, the keyboard (`UITextInput`/`UIKeyInput`, so that the
   system keyboard, its layouts and dictation work), the accessory bar, gestures, the app's
   lifecycle, the clipboard, notifications.

### 5.3 How much is Arbace

Arbace's goal is Arbace down to the ISA, so the question is how thin the non-Arbace part can be.

- (a) **Arbace on the Go route, with no Swift**: the program built for `ios/arm64` as a
  `c-archive`, an app whose entry is Go, UIKit reached through the Objective-C runtime
  (`objc_msgSend`) as the box's launcher reaches Hypervisor.framework on the Mac through purego.
  Whether purego (or Go's cgo with a dozen lines of Objective-C) is usable on iOS, and what Xcode
  is still needed for (signing, the asset catalog, the upload), is to measure (§6, X1).
- (b) **A Swift shell around an Arbace core** (gomobile's pattern): the shell owns UIKit and the
  keyboard; the core (terminal, transport, REPL) is Arbace. Faster to a first app; the shell is
  not Arbace.
- (c) **Swift throughout**: not Arbace; excluded by the goal.

### 5.4 A REPL-aware terminal

A terminal first (it runs any shell, the box, and mosh's server), then what a REPL can add over
it, as an optional channel beside the byte stream (an escape sequence of Arbace's own, or a side
channel in the transport): the bar's keys for `(` `)` `[` `]` `{` `}`, structural moves (paredit's
forward and backward over forms), completion from the REPL's namespaces, a form evaluated at the
cursor, results as data that can be folded and browsed, the REPL's history across sessions. The
REPL's own line editing replaces rlwrap there.

## 6. Experiments before deciding (on the Mac, macOS 14, after the milestone's bundle)

- **X1**: a minimal `ios/arm64` Go program in the iOS simulator and on the device: Go's entry,
  one UIKit view, text input from the system keyboard, through purego or cgo; what it needs of
  Xcode.
- **X2**: Arbace's REPL built for `ios/arm64` (the AOT program): its size, startup and memory on
  the phone; what of jrt does not apply (processes, files outside the sandbox).
- **X3**: a mosh client of Arbace's own (from mosh's source and paper) against the host's
  `mosh-server`: the handshake, datagrams, roaming (IR6).
- **X4**: the terminal core on the host against `vttest` and xterm's behavior, headless.
- **X5**: Metal from Go (purego) on macOS, a grid of glyphs at 120 Hz; then on iOS.

## 7. Checks

- The terminal core: headless tests of byte input to grid output, golden screens (as the oracle
  compares outputs), a `vttest` run, Unicode widths against Unicode's tables.
- The transport: interoperation with stock `mosh-server` (and later Arbace's own server), loss
  and reordering simulated, roaming (a changed client address).
- The app: the specification's behaviors (§4) as a checklist run on the device by the user, as
  the Mac's bundle's checklist is (`box/mac/`), until UI tests can drive it.

## 8. Proposals (for the user to decide)

- **IR1** Where the REPL runs: (a) remote first (the host, natively or in the box), the device
  later; (b) the device first; (c) both from the start. Recommended (a): the server side exists,
  and the phone's first job is the terminal.
- **IR2** The platform shell: (a) Arbace on the Go route with no Swift (§5.3 (a)), if X1 shows it
  can be done; (b) a thin Swift shell around an Arbace core. Recommended (a) after X1, with (b)
  as the fallback if UIKit's text input cannot be reached reasonably from Go.
- **IR3** The macOS desktop first: build the terminal and renderer as a macOS app (purego, as the
  launcher; testable without signing or a device), then iOS. Recommended.
- **IR4** The transport: (a) mosh's protocol clean-room (interoperates with existing servers;
  roaming and local echo on a mobile network); (b) SSH (`golang.org/x/crypto/ssh`'s design, a
  dependency); (c) Arbace's own over QUIC or TLS. Recommended (a) for the terminal, (c) later for
  the REPL's side channel.
- **IR5** The terminal core in Arbace, tested headless on the host before any UI. Recommended.
- **IR6** mosh's protocol (§1): (a) Arbace's own client, written with mosh's source and paper as
  the reference, EPL, interoperating with the stock `mosh-server`; (b) mosh's code translated or
  vendored (GPL-3.0: the client, and the program it is linked into, become a GPL work).
  Recommended (a).
- **IR7** The renderer: Core Text first, Metal when the core is done. Recommended.
- **IR8** The REPL-aware features (§5.4) after the terminal is complete.
- **IR9** The font: JetBrains Mono (OFL 1.1, the model's default too) vendored with its license
  in `LICENSE.md` (Apple's SF Mono is not licensed for bundling). Recommended.
- **IR10** Distribution: the user's own devices through a developer account (free provisioning
  lasts 7 days; a paid account gives a year and TestFlight). The user's choice; the App Store is
  not a goal.
- **IR11** Where it lives: `arbace/term/` (the core, the transport) and `ios/` (the shell and
  packaging), with a survey-and-notes pair as the box had. Recommended.
- **IR12** Order: after `arbace-inside-itself-v1` and S9 (Go's toolchain as forms), or beside
  S9. The user's choice.
- **IR13** The agent features (§2, Agents): notifications, an inbox of questions to approve, a
  chat composer, for the REPL (a long evaluation done, a future finished, the REPL asking). After
  the terminal. Recommended.

## 9. Stages (if IR1-IR13 as recommended)

- **T1** the specification from observation (screenshots, recordings, our servers' side).
- **T2** the terminal core in Arbace, headless, `vttest`.
- **T3** the mosh client of Arbace's own (IR6 (a)), against the host's `mosh-server`.
- **T4** the macOS app: Core Text, then Metal; the accessory bar's keys as a toolbar.
- **T5** X1 then the iOS shell: the system keyboard, the accessory bar, gestures, lifecycle.
- **T6** the REPL-aware features and the agent features (IR13); the REPL on the device (X2).
