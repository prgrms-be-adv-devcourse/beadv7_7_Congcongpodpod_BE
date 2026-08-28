type MemberBenefitsListener = () => void;

const listeners = new Set<MemberBenefitsListener>();

export function notifyMemberBenefitsChanged() {
  listeners.forEach((listener) => listener());
}

export function subscribeMemberBenefitsChanged(listener: MemberBenefitsListener) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}
