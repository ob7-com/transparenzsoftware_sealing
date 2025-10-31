# SAFESealing Design System

## Übersicht

Das SAFESealing Design System basiert auf dem **"underblush"** Theme von DesiUI und nutzt:
- **Tailwind CSS** für Utility-Klassen
- **daisyUI** als Component Library
- **Zentrale Theme-Definition** in `theme.css`

## Verwendung in neuen Seiten

### 1. HTML-Struktur

Jede neue HTML-Seite sollte folgendes Grundgerüst haben:

```html
<!doctype html>
<html lang="de" data-theme="underblush">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Seitentitel – SAFESealing</title>
  
  <!-- Tailwind CSS -->
  <script src="https://cdn.tailwindcss.com"></script>
  
  <!-- daisyUI Component Library -->
  <link href="https://cdn.jsdelivr.net/npm/daisyui@4.12.10/dist/full.min.css" rel="stylesheet" type="text/css" />
  
  <!-- Zentrale Theme-Definition -->
  <link href="/theme.css" rel="stylesheet" type="text/css" />
</head>
<body class="bg-base-200 min-h-screen">
  <!-- Dein Content hier -->
</body>
</html>
```

### 2. Wichtigste Design-Komponenten

#### Buttons
```html
<button class="btn btn-primary">Primary Button</button>
<button class="btn btn-secondary">Secondary Button</button>
<button class="btn btn-outline">Outline Button</button>
```

#### Cards
```html
<div class="card bg-base-100 shadow-lg border border-base-300">
  <div class="card-body">
    <h2 class="card-title">Card Title</h2>
    <p>Card content</p>
  </div>
</div>
```

#### Forms
```html
<div class="form-control">
  <label class="label">
    <span class="label-text">Label</span>
  </label>
  <input type="text" class="input input-bordered w-full" />
</div>
```

#### Status Badges
```html
<span class="badge badge-success">Success</span>
<span class="badge badge-warning">Warning</span>
<span class="badge badge-error">Error</span>
```

### 3. Theme-Farben

Das "underblush" Theme verwendet folgende Farbpalette:

- **Base**: Warmes Beige (229 212 182) - Haupt-Hintergrund
- **Primary**: Pfirsich/Orange (250 169 112) - Haupt-Aktionen
- **Secondary**: Orange (245 79 27) - Sekundäre Aktionen
- **Accent**: Gold (227 198 110) - Akzente
- **Content**: Dunkelblau (30 34 60) - Text und Inhalte
- **Error**: Blau-Grau (74 116 164) - Fehlermeldungen

### 4. Border Radius

Alle Komponenten verwenden einheitlich **2rem** Border Radius für abgerundete Ecken.

## Anpassung des Themes

Falls das Theme angepasst werden soll, bearbeite die Datei:
- `src/main/resources/public/theme.css`

Die Änderungen gelten automatisch für alle Seiten, die `theme.css` einbinden.

## Best Practices

1. **Einheitlichkeit**: Nutze immer die daisyUI-Komponenten für Buttons, Cards, Forms, etc.
2. **Responsive Design**: Nutze Tailwind's responsive Utilities (`sm:`, `md:`, `lg:`, `xl:`)
3. **Spacing**: Nutze Tailwind's Spacing-System für konsistente Abstände
4. **Semantik**: Verwende semantische HTML-Elemente
5. **Accessibility**: Stelle sicher, dass alle interaktiven Elemente keyboard-navigierbar sind

## Weitere Ressourcen

- [Tailwind CSS Dokumentation](https://tailwindcss.com/docs)
- [daisyUI Dokumentation](https://daisyui.com/)
- [daisyUI Components](https://daisyui.com/components/)

