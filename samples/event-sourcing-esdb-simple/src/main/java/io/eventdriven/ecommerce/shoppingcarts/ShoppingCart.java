package io.eventdriven.ecommerce.shoppingcarts;

import io.eventdriven.ecommerce.shoppingcarts.productitems.ProductItems;
import io.eventdriven.ecommerce.shoppingcarts.ShoppingCartEvent.ShoppingCartOpened;
import io.eventdriven.ecommerce.shoppingcarts.ShoppingCartEvent.ProductItemAddedToShoppingCart;
import io.eventdriven.ecommerce.shoppingcarts.ShoppingCartEvent.ProductItemRemovedFromShoppingCart;
import io.eventdriven.ecommerce.shoppingcarts.ShoppingCartEvent.ShoppingCartConfirmed;
import io.eventdriven.ecommerce.shoppingcarts.ShoppingCartEvent.ShoppingCartCanceled;

import java.time.LocalDateTime;
import java.util.UUID;

sealed public interface ShoppingCart {
  UUID id();

  UUID clientId();

  ProductItems productItems();

  record PendingShoppingCart(
    UUID id,
    UUID clientId,
    ProductItems productItems
  ) implements ShoppingCart {
  }

  record ConfirmedShoppingCart(
    UUID id,
    UUID clientId,
    ProductItems productItems,
    LocalDateTime confirmedAt
  ) implements ShoppingCart {
  }

  record CanceledShoppingCart(
    UUID id,
    UUID clientId,
    ProductItems productItems,
    LocalDateTime canceledAt
  ) implements ShoppingCart {
  }

  enum Status {
    Pending,
    Confirmed,
    Cancelled;
  }

  default boolean isClosed() {
    return this instanceof ConfirmedShoppingCart || this instanceof CanceledShoppingCart;
  }

  default ShoppingCart.Status status() {
    return switch (this) {
      case PendingShoppingCart pendingShoppingCart:
        yield Status.Pending;
      case ConfirmedShoppingCart confirmedShoppingCart:
        yield Status.Confirmed;
      case CanceledShoppingCart canceledShoppingCart:
        yield Status.Cancelled;
    };
  }

  static ShoppingCart empty() {
    return new PendingShoppingCart(null, null, null);
  }

  static String mapToStreamId(UUID shoppingCartId) {
    return "ShoppingCart-%s".formatted(shoppingCartId);
  }

  static ShoppingCart when(ShoppingCart current, ShoppingCartEvent
    event) {
    return switch (event) {
      case ShoppingCartOpened shoppingCartOpened:
        yield new PendingShoppingCart(
          shoppingCartOpened.shoppingCartId(),
          shoppingCartOpened.clientId(),
          ProductItems.empty()
        );
      case ProductItemAddedToShoppingCart productItemAddedToShoppingCart:
        yield new PendingShoppingCart(
          current.id(),
          current.clientId(),
          current.productItems().add(productItemAddedToShoppingCart.productItem())
        );
      case ProductItemRemovedFromShoppingCart productItemRemovedFromShoppingCart:
        yield new PendingShoppingCart(
          current.id(),
          current.clientId(),
          current.productItems().remove(productItemRemovedFromShoppingCart.productItem())
        );
      case ShoppingCartConfirmed shoppingCartConfirmed:
        yield new ConfirmedShoppingCart(
          current.id(),
          current.clientId(),
          current.productItems(),
          shoppingCartConfirmed.confirmedAt()
        );
      case ShoppingCartCanceled shoppingCartCanceled:
        yield new CanceledShoppingCart(
          current.id(),
          current.clientId(),
          current.productItems(),
          shoppingCartCanceled.canceledAt()
        );
      case null:
        throw new IllegalArgumentException("Event cannot be null!");
    };
  }
}
