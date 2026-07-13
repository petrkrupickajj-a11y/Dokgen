// Prepnuti jazyka appky (?lang=cs|en|de, viz LocaleConfig) - zachova aktualni
// stranku i pripadne existujici query parametry (napr. filtr v historii),
// jen prepise/prida parametr "lang".
document.querySelectorAll('.jazyky a[data-lang]').forEach(function (odkaz) {
    odkaz.addEventListener('click', function (event) {
        event.preventDefault();
        var url = new URL(window.location.href);
        url.searchParams.set('lang', odkaz.getAttribute('data-lang'));
        window.location.href = url.toString();
    });
});
