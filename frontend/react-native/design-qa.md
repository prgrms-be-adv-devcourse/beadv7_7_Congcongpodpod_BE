# Design QA

- Source visual truth: `/Users/oxxultus/.codex/visualizations/2026/08/09/019fe759-cf86-7032-8449-a095270117a1/lastdish-screen-mockups.html`
- Implementation captures: `/private/tmp/lastdish-premium-home.png`, `/private/tmp/lastdish-premium-stores.png`, `/private/tmp/lastdish-premium-store.png`, `/private/tmp/lastdish-premium-product.png`, `/private/tmp/lastdish-premium-cart-empty.png`, `/private/tmp/lastdish-responsive-orders.png`, `/private/tmp/lastdish-responsive-order-detail.png`, `/private/tmp/lastdish-responsive-deposits.png`, `/private/tmp/lastdish-my-redesign.png`
- Viewport: iOS simulator, 393 × 852 CSS points, 1179 × 2556 pixels, 3× density
- State: logged out, home and nearby-store list, production API

## Full-view comparison

Home preserves the selected information architecture: separate search/notification/cart controls, map-overlaid compact filters, full map, visible user-location marker, zoom/recenter controls, and five-item bottom navigation. Orders, order detail, and deposit now follow the mockup's distinct card, timeline, receipt, and wallet patterns instead of sharing a generic page layout.

## Focused checks

- Typography: Apple SD Gothic Neo on iOS and the Korean system sans-serif on Android; reduced heavy weights and no clipped labels in captured states.
- Spacing: 16–18 point side rhythm, compact controls, persistent tab bar, safe areas respected.
- Colors: neutral canvas with pastel green accent; active and semantic states remain distinct.
- Images: API currently returns null thumbnails; icon-backed neutral fallbacks are used until store and dish images exist.
- Copy: route titles, pickup language, order states, deposit and seller actions match the mockup intent.

## Comparison history

1. P0: store list empty because the client expected `content`, while the API returns `data.stores`. Fixed with response-envelope and field adapters. Evidence: `lastdish-rn-stores.png` shows 18 real stores.
2. P1: map showed no stores because camera followed the simulator location while demo rows were outside that area. Fixed by separating camera center from the user-location overlay.
3. P2: custom React marker children rendered at incorrect native scale. Replaced them with Naver native markers and native clustering; overlapping demo coordinates now collapse to a numbered cluster. Evidence: `lastdish-polish-home.png`.
4. P2: seller navigation remounted with every screen because it lived inside each screen shell. Moved it to the persistent `/seller` Tabs layout while preserving the accepted visual design. Evidence: `lastdish-polish-seller.png`.
5. P0: device GPS was overwritten by the nearby-store API fallback location. The fallback now affects only the query; the blue device marker remains visible. Evidence: `lastdish-responsive-home.png`.
6. P1: the location control had no repeatable camera command and the map exposed no zoom buttons. Added imperative Naver camera commands for recenter, zoom in, and zoom out with a 6–19 zoom clamp.
7. P1: orders, order detail, and deposit used generic containers that diverged from the mock. Rebuilt them around the mock's order timeline, pickup-code panel, receipt hierarchy, wallet card, and grouped transaction rows.
8. P1: My Page omitted the mock's quick counts and grouped activity, seller, and support navigation. Rebuilt both authenticated and guest states; the authenticated capture preserves real member data while matching the mock's hierarchy. Evidence: `lastdish-my-redesign.png`.
9. P1: nearby-store location was visually detached from the map filters. It now shares the same chip treatment and sits immediately left of “지금 픽업”. Evidence: `lastdish-premium-home.png`.
10. P1: store list, store detail, product detail, and cart used unrelated generic card patterns. Rebuilt the purchase path around dense store rows, image-led details, consistent stock badges, location/menu hierarchy, persistent purchase actions, and explicit empty/filled cart states.
11. P0: product CTA navigated to a static mock cart without adding the selected product. Added shared cart state, real selected-product insertion, quantity bounds, removal, total calculation, checkout handoff, and a live home cart badge.

## Interaction checks

- Radius selector updates query and summary.
- Home recenter emits a new command on every press; map zoom controls animate one level per press.
- Store cards navigate to detail; product rows navigate to product detail.
- Cart → checkout, order → detail, deposit charge result, seller tabs, stock batch-add, and settlement month controls are wired.
- TypeScript and ESLint pass.

final result: passed
