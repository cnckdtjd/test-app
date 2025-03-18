// Main JavaScript file for Test App

document.addEventListener('DOMContentLoaded', function() {
    // Auto-hide alerts after 5 seconds
    setTimeout(function() {
        const alerts = document.querySelectorAll('.alert');
        alerts.forEach(function(alert) {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        });
    }, 5000);

    // Cart functionality
    setupCartFunctionality();
    
    // Payment processing
    setupPaymentProcessing();
});

function setupCartFunctionality() {
    // Add to cart buttons
    const addToCartButtons = document.querySelectorAll('.add-to-cart-btn');
    addToCartButtons.forEach(function(button) {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const productId = this.getAttribute('data-product-id');
            const quantity = document.querySelector('#quantity-' + productId)?.value || 1;
            
            addToCart(productId, quantity);
        });
    });
    
    // Update quantity buttons
    const updateQuantityButtons = document.querySelectorAll('.update-quantity-btn');
    updateQuantityButtons.forEach(function(button) {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const cartId = this.getAttribute('data-cart-id');
            const productId = this.getAttribute('data-product-id');
            const quantity = document.querySelector('#quantity-' + productId).value;
            
            updateCartItemQuantity(cartId, productId, quantity);
        });
    });
    
    // Remove from cart buttons
    const removeFromCartButtons = document.querySelectorAll('.remove-from-cart-btn');
    removeFromCartButtons.forEach(function(button) {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const cartId = this.getAttribute('data-cart-id');
            const productId = this.getAttribute('data-product-id');
            
            removeFromCart(cartId, productId);
        });
    });
    
    // Clear cart button
    const clearCartButton = document.querySelector('.clear-cart-btn');
    if (clearCartButton) {
        clearCartButton.addEventListener('click', function(e) {
            e.preventDefault();
            const cartId = this.getAttribute('data-cart-id');
            
            clearCart(cartId);
        });
    }
}

function addToCart(productId, quantity) {
    fetch('/cart/add', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        },
        body: `productId=${productId}&quantity=${quantity}`
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert('success', data.message);
        } else {
            showAlert('danger', data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('danger', 'An error occurred while adding to cart');
    });
}

function updateCartItemQuantity(cartId, productId, quantity) {
    fetch('/cart/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        },
        body: `cartId=${cartId}&productId=${productId}&quantity=${quantity}`
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert('success', data.message);
            // Reload the page to update the cart
            window.location.reload();
        } else {
            showAlert('danger', data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('danger', 'An error occurred while updating cart');
    });
}

function removeFromCart(cartId, productId) {
    fetch('/cart/remove', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        },
        body: `cartId=${cartId}&productId=${productId}`
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert('success', data.message);
            // Reload the page to update the cart
            window.location.reload();
        } else {
            showAlert('danger', data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('danger', 'An error occurred while removing from cart');
    });
}

function clearCart(cartId) {
    fetch('/cart/clear', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        },
        body: `cartId=${cartId}`
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert('success', data.message);
            // Reload the page to update the cart
            window.location.reload();
        } else {
            showAlert('danger', data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('danger', 'An error occurred while clearing cart');
    });
}

function setupPaymentProcessing() {
    const paymentButton = document.querySelector('.process-payment-btn');
    if (paymentButton) {
        paymentButton.addEventListener('click', function(e) {
            e.preventDefault();
            const orderId = this.getAttribute('data-order-id');
            
            processPayment(orderId);
        });
    }
}

function processPayment(orderId) {
    // Show loading state
    const paymentButton = document.querySelector('.process-payment-btn');
    if (paymentButton) {
        paymentButton.disabled = true;
        paymentButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...';
    }
    
    fetch(`/orders/${orderId}/pay`, {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert('success', data.message);
            // Redirect to order details page after successful payment
            window.location.href = `/orders/${orderId}`;
        } else {
            showAlert('danger', data.message);
            // Reset button state
            if (paymentButton) {
                paymentButton.disabled = false;
                paymentButton.innerHTML = 'Process Payment';
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showAlert('danger', 'An error occurred while processing payment');
        // Reset button state
        if (paymentButton) {
            paymentButton.disabled = false;
            paymentButton.innerHTML = 'Process Payment';
        }
    });
}

function showAlert(type, message) {
    const alertsContainer = document.createElement('div');
    alertsContainer.className = 'container mt-3';
    alertsContainer.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
    
    // Insert after header
    const header = document.querySelector('header');
    if (header && header.nextSibling) {
        header.parentNode.insertBefore(alertsContainer, header.nextSibling);
    } else {
        document.body.prepend(alertsContainer);
    }
    
    // Auto-hide after 5 seconds
    setTimeout(function() {
        const alert = alertsContainer.querySelector('.alert');
        if (alert) {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }
    }, 5000);
} 