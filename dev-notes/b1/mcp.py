#!/usr/bin/env python3
"""Minimal MCP client for the ChurchApps Api. Usage:
     mcp.py <tool> '<json args>'      e.g. mcp.py describe_page_builder '{}'
     mcp.py api GET /content/pages    convenience wrapper for api_call
"""
import json, sys, urllib.request, pathlib

KEY = pathlib.Path.home().joinpath(".config/mtz/b1-mcp-key").read_text().strip()
URL = "https://b1api.mtzcg.com/mcp"
_id = [0]

def call(tool, args):
    _id[0] += 1
    payload = {"jsonrpc": "2.0", "id": _id[0], "method": "tools/call",
               "params": {"name": tool, "arguments": args}}
    req = urllib.request.Request(URL, data=json.dumps(payload).encode(),
        headers={"Authorization": f"Bearer {KEY}", "Content-Type": "application/json",
                 "Accept": "application/json, text/event-stream",
                 "User-Agent": "curl/8.7.1"})
    d = json.loads(urllib.request.urlopen(req).read())
    if "error" in d:
        raise SystemExit("JSONRPC ERROR: " + json.dumps(d["error"])[:400])
    return "\n".join(c.get("text", "") for c in d["result"].get("content", []))

def api(method, path, body=None, query=None):
    args = {"method": method, "path": path}
    if body is not None:  args["body"] = body
    if query is not None: args["query"] = query
    out = call("api_call", args)
    try:    return json.loads(out)
    except Exception: return {"raw": out}

if __name__ == "__main__":
    if sys.argv[1] == "api":
        method, path = sys.argv[2], sys.argv[3]
        body = json.loads(sys.argv[4]) if len(sys.argv) > 4 else None
        print(json.dumps(api(method, path, body), indent=1)[:3000])
    else:
        print(call(sys.argv[1], json.loads(sys.argv[2] if len(sys.argv) > 2 else "{}")))
