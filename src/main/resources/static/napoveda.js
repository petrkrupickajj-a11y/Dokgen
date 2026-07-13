// Prepina viditelnost napovedneho panelu po kliknuti na plovouci tlacitko v rohu stranky.
document.querySelectorAll('.napoveda-tlacitko').forEach(function (tlacitko) {
    var panel = document.getElementById(tlacitko.getAttribute('aria-controls'));
    if (!panel) {
        return;
    }

    tlacitko.addEventListener('click', function () {
        var otevirame = panel.hasAttribute('hidden');
        if (otevirame) {
            panel.removeAttribute('hidden');
        } else {
            panel.setAttribute('hidden', '');
        }
        tlacitko.setAttribute('aria-expanded', String(otevirame));
    });

    var zavrit = panel.querySelector('.zavrit');
    if (zavrit) {
        zavrit.addEventListener('click', function () {
            panel.setAttribute('hidden', '');
            tlacitko.setAttribute('aria-expanded', 'false');
        });
    }
});
