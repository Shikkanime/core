// When hover on anime card, show description on the anime card
const hoverCards = document.querySelectorAll('.hover-card');

hoverCards.forEach((animeCard) => {
    const element = animeCard.querySelector('.hover-card-description');

    if (element === null) {
        return;
    }

    animeCard.addEventListener('mouseover', () => {
        element.classList.remove('d-none');
    });

    animeCard.addEventListener('mouseout', () => {
        element.classList.add('d-none');
    });
});