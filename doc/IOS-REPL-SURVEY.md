# A touch terminal for Arbace's REPL: a survey and plan

Status: survey and plan for discussion, 2026-10-14, long term (after `arbace-inside-itself-v1`;
the agenda's Later). Nothing here is built. The proposals (IR1-IR12, §8) are the user's to
decide. The user offered more screenshots of the model app; §3 lists what they would answer.

## Why

The user reaches the host from an iPhone through Moshi, a mosh terminal for iOS, over a mobile
network. The goal of the standalone Arbace is a self-sustaining REPL in a sandbox, in Arbace
down to the ISA; its interface today is a cooked console under rlwrap (go/EXEC-NOTES.md, "Line
editing: rlwrap"; go/BOX-NOTES.md, S1). A touch interface of Arbace's own, as good as Moshi's on
a phone, is the REPL's first interface beyond a terminal. The user named Moshi's tooling as one
to study (AGENDA, Later): a Go backend on Linux and macOS and a GPU-accelerated Go frontend on
macOS, confirmed by the user's measurements; the iOS app's tooling is not known.

## 1. Clean room

A clean-room reimplementation keeps two roles apart:

- **The specification side** observes the model app only as a user does: screenshots, screen
  recordings, its public pages and App Store text, and the bytes it sends on the network to a
  server we run. It writes a functional specification (§4) with no code, no assets and no
  wording copied from the app.
- **The implementation side** builds from that specification and from public standards and
  papers alone. It never sees the app's binaries, its resources or decompiled code.

Consequences:

- The bundle inspection suggested on 2026-10-14 (Apple Configurator, `otool -L`) tells only
  which tooling the app uses. If it is done, it is done by the specification side, and the
  record keeps only that fact (Swift or Go, which frameworks), not symbols or code.
- No artwork, icons, fonts, colors taken from its themes, or names (the product's name stays
  out of the UI and the code; this document names it as the model).
- mosh (the protocol and its reference implementation, GPL-3.0 with an OpenSSL exception): if
  Arbace speaks mosh's protocol, it is implemented from the published description (Winstein and
  Balakrishnan, "Mosh: An Interactive Remote Shell for Mobile Clients", USENIX ATC 2012) and
  from observing the wire against the host's stock `mosh-server` (Alpine's `mosh`), not from
  mosh's source, so that the result can be EPL as the rest of Arbace (`LICENSE.md`). The
  interoperability tests run against that binary. If the paper is not enough, reading the source
  is a decision with a license cost (IR6).
- The terminal's escape sequences come from public references (ECMA-48, the DEC VT100/VT220
  manuals, xterm's "ctlseqs" document), which every emulator uses.

## 2. What is observed so far (2026-10-14)

From one screenshot (a session on the host, `free`):

- A full-screen dark terminal on a sheet with a grab handle; above the text, a row with two
  round buttons (a yellow one with a minus: minimize or close the session; a green one with a
  list: the sessions), the host's name, and on the right a badge naming the transport ("Mosh").
- The terminal: a monospace font with a slashed zero, ANSI colors in the prompt (green host,
  yellow path), a block cursor, about 46 columns in portrait, the output wrapped at the width.
- Above the system keyboard, a rounded accessory bar, scrollable: Ctrl, Esc, Tab, a cross of
  four (arrows as a pad, presumably), a list (sessions or a command list), a clipboard (paste),
  then a curved arrow (history or undo), a speech bubble (a chat or an agent prompt), and a
  hide-the-keyboard key.
- The system keyboard (here the Hungarian QWERTZ layout, with the globe and dictation keys):
  the app takes system input, so dictation and the user's layouts work.

From the host's side: the session's server is the stock `mosh-server` (`new -s -c 256`), so the
app is a mosh client, 256 colors.

## 3. What more screenshots would answer

1. The session list and how a session is created (host, user, key, transport choices).
2. The pad behind the cross of four, and what Ctrl does (a latch for the next key, or held).
3. The list key and the curved arrow: what they open.
4. The speech bubble: a chat with an agent, a composer for a long line, or dictation.
5. Selection and copy: how text is selected on the grid; scrollback by swipe; pinch to zoom.
6. Landscape, an external keyboard, the iPad if any.
7. Disconnection and roaming: what the screen shows on a lost network (mosh's "last contact").
8. Settings: fonts, themes, key mappings, notifications (a bell, a finished command).

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
- **X3**: mosh's handshake and datagrams against the host's `mosh-server`, from the paper: is
  the paper enough for a client that interoperates (IR6).
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
- **IR6** The clean-room boundary as §1: the specification from observation, mosh from the
  paper and the wire; reading mosh's source only by a later decision. Recommended.
- **IR7** The renderer: Core Text first, Metal when the core is done. Recommended.
- **IR8** The REPL-aware features (§5.4) after the terminal is complete.
- **IR9** The font: a monospace font under the OFL vendored with its license in `LICENSE.md`
  (Apple's SF Mono is not licensed for bundling). Which one is open.
- **IR10** Distribution: the user's own devices through a developer account (free provisioning
  lasts 7 days; a paid account gives a year and TestFlight). The user's choice; the App Store is
  not a goal.
- **IR11** Where it lives: `arbace/term/` (the core, the transport) and `ios/` (the shell and
  packaging), with a survey-and-notes pair as the box had. Recommended.
- **IR12** Order: after `arbace-inside-itself-v1` and S9 (Go's toolchain as forms), or beside
  S9. The user's choice.

## 9. Stages (if IR1-IR12 as recommended)

- **T1** the specification from observation (screenshots, recordings, the wire).
- **T2** the terminal core in Arbace, headless, `vttest`.
- **T3** the mosh client clean-room, against the host's `mosh-server`.
- **T4** the macOS app: Core Text, then Metal; the accessory bar's keys as a toolbar.
- **T5** X1 then the iOS shell: the system keyboard, the accessory bar, gestures, lifecycle.
- **T6** the REPL-aware features; the REPL on the device (X2).
