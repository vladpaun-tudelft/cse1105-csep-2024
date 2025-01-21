const tooltip = document.getElementById('tooltip');

document.querySelectorAll('.note-link').forEach(element => {
    element.addEventListener('mouseover', (event) => {
        showTooltip(event, element);
    });
    element.addEventListener('mousemove', (event) => {
        positionTooltip(event);
    });
    element.addEventListener('mouseout', hideTooltip);
});

document.querySelectorAll('a').forEach(element => {
    element.addEventListener('click', (event) => {
        event.preventDefault();
        const href = element.getAttribute('href');
        window.alert(href);
    })
})

function showTooltip(event, element) {
    const noteTitle = element.getAttribute('data-note-title');
    const noteCollection = element.getAttribute('data-note-collection');
    const notePreview = element.getAttribute('data-note-preview');

    // fallback if something fails
    const localizedStrings = window.localizedStrings || {
        collectionLabel: 'Collection',
        noteLabel: 'Note',
        previewLabel: 'Preview'
    };

    tooltip.innerHTML =
        `${localizedStrings.collectionLabel}: <b>${noteCollection}</b>;<br>` +
        `${localizedStrings.noteLabel}: <b>${noteTitle}</b>;<br>` +
        `${localizedStrings.previewLabel}: ${notePreview}`;
    tooltip.style.display = 'block';
    positionTooltip(event);
}

function positionTooltip(event) {
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const tooltipRect = tooltip.getBoundingClientRect();
    let left = event.pageX + 10;
    let top = event.pageY + 20;

    if (left + tooltipRect.width > viewportWidth) {
        left = viewportWidth - tooltipRect.width - 10;
    }
    if (top + tooltipRect.height > viewportHeight) {
        top = viewportHeight - tooltipRect.height - 10;
    }

    tooltip.style.left = left + 'px';
    tooltip.style.top = top + 'px';
}

function hideTooltip() {
    tooltip.style.display = 'none';
}

function handleTagClick(tag) {
    window.alert('tag://' + tag);
}
