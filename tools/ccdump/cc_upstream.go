//go:build upstream

package main

import cc "modernc.org/cc/v4"

// upstream modernc.org/cc/v4 v4.29.7 (the build tag upstream): it has no SetHost, so the configuration is
// made as NewConfig makes it from a compiler's answers, without asking one.

type (
	Token  = cc.Token
	Config = cc.Config
	Source = cc.Source
)

const flavor = "upstream"

var (
	arbaceLex        = cc.ArbaceLex
	arbacePreprocess = cc.ArbacePreprocess
)

func newConfig(h *host) (*Config, error) {
	abi, err := cc.NewABI("linux", "amd64")
	if err != nil {
		return nil, err
	}
	return &Config{
		ABI:                 abi,
		CC:                  "gcc",
		HostIncludePaths:    h.include,
		HostSysIncludePaths: h.sysInclude,
		IncludePaths:        append([]string{""}, append(append([]string(nil), h.include...), h.sysInclude...)...),
		Predefined:          h.predefined,
		SysIncludePaths:     h.sysInclude,
	}, nil
}
