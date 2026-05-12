# Output Formats

All output files are UTF-8 and generated from the same guarded query result.

## `xml`
- Structure: `<export><data><property path="..." type="..."><value>...</value></property></data></export>`
- Preserves JDBC type names per field.

## `json`
- Pretty-printed JSON array.
- Each row contains per-column objects:
  - `value`
  - `type`

Example:
```json
[
  {
    "ID": {"value": "1", "type": "INTEGER"},
    "NAME": {"value": "Alice", "type": "VARCHAR"}
  }
]
```

## `jsonl`
- One JSON object per line.
- Best for streaming/pipeline ingestion.

Example:
```json
{"ID":1,"NAME":"Alice"}
{"ID":2,"NAME":"Bob"}
```

## `csv`
- Semicolon-delimited (`;`).
- Line 1: column names.
- Line 2: JDBC type names.
- Following lines: row values.

## `md`
- Markdown table.
- Escapes pipe characters in values.

## `html`
- Fully self-contained HTML5 document with embedded CSS and responsive table.
- Column headers include JDBC type tooltip.
- Light/dark mode friendly defaults.
- File extension: `.html`
- Config:
  - `zeus.ibmi.output.html.theme`: `light` | `dark` | `auto` (default: `auto`)
  - `zeus.ibmi.output.html.custom-css-file`: optional path to custom CSS to append
  - `zeus.ibmi.output.html.include-manifest`: include embedded manifest block in HTML (default: `true`)

## Selection
Use `--output-formats` as comma-separated values, e.g.:
```bash
--output-formats xml,json,jsonl,csv,md,html
```
