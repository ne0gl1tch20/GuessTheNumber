// Example presentation-only module.
// The host passes a localized view model. No direct save/reward access is exposed.
export function renderTrait(viewModel) {
  return {
    title: viewModel.title,
    description: viewModel.description,
    progressLabel: viewModel.progressLabel ?? ""
  };
}
