package net.spacealtctrl.discordrp.ui.screens.cover

internal object CoverAttach {
    private val BASE64 = Regex("^[A-Za-z0-9+/]+={0,2}$")

    fun script(jpegBase64: String): String? {
        if (!BASE64.matches(jpegBase64)) return null
        return TEMPLATE.replace(PLACEHOLDER, jpegBase64)
    }

    private const val PLACEHOLDER = "%COVER%"

    private val TEMPLATE = """
        (function () {
          var input = document.querySelector('input.add-files');
          if (!input) return 'no-form';
          if (input.getAttribute('data-discordrp') === 'attached') return 'already';
          var bytes = atob('%COVER%');
          var buffer = new Uint8Array(bytes.length);
          for (var i = 0; i < bytes.length; i++) buffer[i] = bytes.charCodeAt(i);
          var transfer = new DataTransfer();
          transfer.items.add(new File([buffer], 'cover.jpg', { type: 'image/jpeg' }));
          input.files = transfer.files;
          input.setAttribute('data-discordrp', 'attached');
          input.dispatchEvent(new Event('change', { bubbles: true }));
          var tries = 0;
          (function tickFront() {
            var front = document.querySelector('ul.cover-art-type-checkboxes input.type[value="1"]');
            if (front) {
              if (!front.checked) front.click();
              var enter = document.getElementById('add-cover-art-submit');
              if (enter) enter.scrollIntoView({ block: 'center' });
              return;
            }
            if (++tries < 50) setTimeout(tickFront, 100);
          })();
          return 'attached';
        })();
    """.trimIndent()
}
