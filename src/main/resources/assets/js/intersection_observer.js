const images = document.querySelectorAll('img[loading="lazy"]');

const observer = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
        if (entry.isIntersecting) {
            const image = entry.target;
            image.src = image.dataset.src;
            observer.unobserve(image);
        }
    });
});

images.forEach((image) => {
    observer.observe(image);
});
