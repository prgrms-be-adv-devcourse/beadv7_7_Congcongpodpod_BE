const prefix = 'lastdish.';

function browserStorage() {
  return typeof window === 'undefined' ? undefined : window.localStorage;
}

export const storage = {
  async getItem(key: string) {
    return browserStorage()?.getItem(`${prefix}${key}`) ?? null;
  },
  async setItem(key: string, value: string) {
    browserStorage()?.setItem(`${prefix}${key}`, value);
  },
  async deleteItem(key: string) {
    browserStorage()?.removeItem(`${prefix}${key}`);
  },
};
