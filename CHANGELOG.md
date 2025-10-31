# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- Support for multiple value elements (Transaction.Begin and Transaction.End) in XML verification
- Enhanced measurement data extraction: now correctly processes both Begin and End transactions
- Test suite for multiple transaction XML files (`VerifyMultipleTransactionsTest`)
- Comprehensive display of all transaction information:
  - Energy delivered (calculated from start and end meter readings)
  - Start and end meter readings with timestamps
  - Charging duration calculation
  - Meter serial number
  - All metadata from OCMF signed data

### Fixed
- VerifyController now correctly processes XML files with both Transaction.Begin and Transaction.End value elements
- Previously only the first signedData block was processed, missing the end transaction data
- Energy delivered values and end meter readings are now correctly extracted and displayed

### Changed
- Modern UI redesign with Tailwind CSS and daisyUI component library
- Professional Handelsblatt-style color theme (light, business-oriented design)
- Docker support with Dockerfile and .dockerignore for containerized deployment
- Enhanced user interface with:
  - Icons and improved visual hierarchy
  - Better form controls and labels
  - Professional spacing and layout
  - Responsive design for mobile, tablet, and desktop
  - Status badges for health checks
  - Mockup-code styling for JSON outputs
- OB7 Logo integration in sidebar navigation (clickable, returns to homepage)
- Google Font (Inter) integration for consistent typography

### Changed
- Complete UI overhaul of web demo interface (`index.html`)
- Improved UX with better visual feedback and clearer structure
- Enhanced form styling and component design following daisyUI best practices
- Sidebar header simplified: logo with "Transparenz Software" text
- Logo now uses PNG format instead of SVG

### Removed
- Health status badge from sidebar (simplified UI)

### Technical Details
- Added Tailwind CSS Play CDN for styling
- Integrated daisyUI 4.12.10 via CDN
- Custom CSS theme variables for professional business appearance
- Maintained all existing JavaScript functionality and API endpoints
- Inter font family integrated in theme.css for global typography

