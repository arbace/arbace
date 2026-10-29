//go:build !upstream

package main

import cc "github.com/arbace/go-whim/crefactor/cc"

// go-whim's crefactor/cc (doc/C23-SURVEY.md, C1.3): the configuration through go-whim's SetHost, so
// that cc never asks a compiler.

type (
	Token  = cc.Token
	Config = cc.Config
	Source = cc.Source
)

const flavor = "go-whim"

var (
	arbaceLex        = cc.ArbaceLex
	arbacePreprocess = cc.ArbacePreprocess
)

func newConfig(h *host) (*Config, error) {
	hostMu.Lock()
	defer hostMu.Unlock()
	cc.SetHost(&cc.Host{CC: "gcc", Predefined: h.predefined, IncludePaths: h.include, SysIncludePaths: h.sysInclude})
	defer cc.SetHost(nil)
	return cc.NewConfig("linux", "amd64")
}
