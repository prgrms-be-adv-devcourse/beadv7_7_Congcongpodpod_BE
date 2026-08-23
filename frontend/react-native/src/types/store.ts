export type Dish = {
  dishId: number;
  dishName: string;
  description: string;
  price: number;
  discountPrice: number;
  quantity: number;
  status?: string;
  imageUrl?: string;
  registeredAt?: string;
  storeId?: number;
  storeName?: string;
  storeCategory?: string;
};

export type Store = {
  storeId: number;
  memberId?: number;
  storeName: string;
  businessNumber?: string;
  category: string;
  address: string;
  detailAddress?: string;
  phone?: string;
  openTime?: string;
  closeTime?: string;
  status?: string;
  latitude: number;
  longitude: number;
  coverImageUrl?: string;
  profileImageUrl?: string;
  /** @deprecated coverImageUrl/profileImageUrl 호환용 */
  imageUrl?: string;
  dishes: Dish[];
};
