const { div, p, img, button } = van.tags;

const ChapterEnding = () => {
  return () =>
    reader.generalSettings.val.pageReader
      ? div()
      : div(div({ class: 'info-text' }, reader.strings.finished), () =>
          reader.nextChapter
            ? button(
                {
                  class: 'next-button',
                  onclick: e => {
                    e.stopPropagation();
                    reader.post({ type: 'next' });
                  },
                },
                reader.strings.nextChapter,
              )
            : div({ class: 'info-text' }, reader.strings.noNextChapter),
        );
};

const Scrollbar = () => {
  const horizontal = van.derive(
    () => !reader.generalSettings.val.verticalSeekbar,
  );
  let lock = false;
  const percentage = van.state(0);
  let lastPostedPercentage = -1;
  const postPosition = value => {
    const position = Math.max(0, Math.min(100, parseInt(value, 10) || 0));
    if (position === lastPostedPercentage) return;
    lastPostedPercentage = position;
    reader.post({ type: 'position', data: position });
  };
  const update = ratio => {
    if (ratio === undefined) {
      const maxScroll = Math.max(1, reader.chapterHeight - reader.layoutHeight);
      ratio = window.scrollY / maxScroll;
    }
    ratio = Math.max(0, Math.min(1, ratio));
    if (reader.generalSettings.val.pageReader) {
      pageReader.movePage(
        parseInt(pageReader.totalPages.val * Math.min(0.99, ratio)),
      );
      return;
    }
    percentage.val = parseInt(ratio * 100);
    postPosition(percentage.val);
    if (lock) {
      const maxScroll = Math.max(0, reader.chapterHeight - reader.layoutHeight);
      window.scrollTo({
        top: maxScroll * ratio,
        behavior: 'instant',
      });
    }
  };
  window.addEventListener(
    'scroll',
    () => !lock && !reader.generalSettings.val.pageReader && update(),
  );
  return div(
    { id: 'ScrollBar' },
    div(
      { class: 'scrollbar-item scrollbar-text', id: 'scrollbar-percentage' },
      () =>
        reader.generalSettings.val.pageReader
          ? pageReader.page.val + 1
          : percentage.val,
    ),
    div(
      {
        class: 'scrollbar-item',
        id: 'scrollbar-slider',
        role: 'slider',
        'aria-label': 'Progression de lecture',
        'aria-valuemin': '0',
        'aria-valuemax': '100',
        'aria-valuenow': () =>
          reader.generalSettings.val.pageReader && pageReader.totalPages.val > 0
            ? parseInt(
                ((pageReader.page.val + 1) / pageReader.totalPages.val) * 100,
              )
            : percentage.val,
      },
      div(
        { id: 'scrollbar-track' },
        div(
          {
            id: 'scrollbar-progress',
            style: () => {
              const percentageValue = reader.generalSettings.val.pageReader
                ? ((pageReader.page.val + 1) / pageReader.totalPages.val) * 100
                : percentage.val;
              return horizontal.val
                ? `width: ${percentageValue}%; height: 100%;`
                : `height: ${percentageValue}%; width: 100%;`;
            },
          },
          div(
            {
              id: 'scrollbar-thumb-wrapper',
              ontouchstart: () => {
                lock = true;
              },
              ontouchend: () => {
                lock = false;
              },
              ontouchmove: function (e) {
                const slider = this.parentElement.parentElement.parentElement;
                const sliderHeight = horizontal.val
                  ? slider.clientWidth
                  : slider.clientHeight;
                const sliderOffsetY = horizontal.val
                  ? slider.getBoundingClientRect().left
                  : slider.getBoundingClientRect().top;
                const ratio =
                  ((horizontal.val
                    ? e.changedTouches[0].clientX
                    : e.changedTouches[0].clientY) -
                    sliderOffsetY) /
                  sliderHeight;
                update(ratio < 0 ? 0 : ratio);
              },
            },
            div({ id: 'scrollbar-thumb' }),
          ),
        ),
      ),
    ),
    div(
      {
        class: 'scrollbar-item scrollbar-text',
        id: 'scrollbar-percentage-max',
      },
      () =>
        reader.generalSettings.val.pageReader ? pageReader.totalPages.val : 100,
    ),
  );
};

const ToolWrapper = () => {
  const horizontal = van.derive(
    () => !reader.generalSettings.val.verticalSeekbar,
  );
  return div(
    {
      id: 'ToolWrapper',
      class: () =>
        `${reader.hidden.val ? 'hidden' : ''} ${
          horizontal.val ? 'horizontal' : ''
        }`,
    },
    Scrollbar(),
  );
};

const ImageModal = ({ src }) => {
  return div(
    {
      id: 'Image-Modal',
      class: () => (src.val ? 'show' : ''),
      onclick: e => {
        if (e.target.id !== 'Image-Modal-img') {
          e.stopPropagation();
          src.val = '';
        }
      },
    },
    img({
      id: 'Image-Modal-img',
      src: src,
      alt: () => (src.val ? `Cant not render image from ${src.val}` : ''),
    }),
  );
};

const ModalWrapper = () => {
  const imgSrc = van.state('');
  const showImage = src => {
    imgSrc.val = src;
    reader.viewport.setAttribute(
      'content',
      'width=device-width, initial-scale=1.0, maximum-scale=10',
    );
  };
  const hideImage = () => {
    imgSrc.val = '';
    reader.viewport.setAttribute(
      'content',
      'width=device-width, initial-scale=1.0, maximum-scale=1.0',
    );
  };

  document.addEventListener('contextmenu', e => {
    if (e.target instanceof HTMLImageElement) {
      if (!imgSrc.val) {
        showImage(e.target.src);
      } else {
        hideImage();
      }
    }
  });
  return div(ImageModal({ src: imgSrc }));
};

const TTSController = () => {
  let controllerElement = null;
  let hoverElement = null;
  let clientX = null;
  let clientY = null;
  let dragged = false;
  return div(
    {
      id: 'TTS-Controller',
      class: () => `${reader.generalSettings.val.TTSEnable ? '' : 'hidden'}`,
      style: () =>
        reader.generalSettings.val.TTSEnable
          ? 'pointer-events: auto;'
          : 'pointer-events: none; display: none !important; opacity: 0; transition: none;',
      ontouchstart: () => {
        if (!controllerElement) {
          controllerElement = document.getElementById('TTS-Controller');
        }
        controllerElement.classList.add('active');
        controllerElement.style.transition = '';
        controllerElement.style.bottom = 'auto';
        controllerElement.style.transform = 'translate(-50%, -50%)';
        dragged = false;
      },
      ontouchmove: e => {
        e.preventDefault();
        e.stopPropagation();
        dragged = true;
        clientX = e.changedTouches[0].clientX;
        clientY = e.changedTouches[0].clientY;
        controllerElement.style.left = `${clientX}px`;
        controllerElement.style.top = `${clientY}px`;
        const hoverElements = document.elementsFromPoint(clientX, clientY);
        const newHoverElement = hoverElements.reverse().find(e => {
          if (e.id.includes('scrollbar')) {
            return false;
          }
          return tts.readable(e);
        });
        hoverElement?.classList.remove('highlight');
        if (newHoverElement) {
          newHoverElement.classList.add('highlight');
          hoverElement = newHoverElement;
        } else {
          hoverElement = null;
        }
      },
      ontouchend: () => {
        controllerElement.style.transition = '180ms ease';
        controllerElement.classList.remove('active');
        controllerElement.style.left = '50%';
        controllerElement.style.top = 'auto';
        controllerElement.style.bottom =
          'max(88px, calc(env(safe-area-inset-bottom) + 72px))';
        controllerElement.style.transform = 'translateX(-50%)';
        if (clientX && clientY) {
          // Check if TTS is still enabled before starting
          if (hoverElement && reader.generalSettings.val.TTSEnable) {
            tts.start(hoverElement);
            controllerElement.firstElementChild.innerHTML = pauseIcon;
          }
        }
        clientX = null;
        clientY = null;
      },
      onclick: e => {
        e.stopPropagation();
        if (dragged) {
          dragged = false;
          return;
        }
        // Don't allow interaction if TTS is disabled
        if (!reader.generalSettings.val.TTSEnable) {
          return;
        }
        if (tts.reading) {
          tts.pause();
          controllerElement.firstElementChild.innerHTML = resumeIcon;
        } else if (tts.started) {
          tts.resume();
          controllerElement.firstElementChild.innerHTML = pauseIcon;
        } else {
          tts.start();
          controllerElement.firstElementChild.innerHTML = pauseIcon;
        }
      },
    },
    button({
      innerHTML: volumnIcon,
      'aria-label': 'Lecture vocale',
    }),
  );
};

const ReaderUI = () => {
  return div(
    TTSController(),
    ModalWrapper(),
    ChapterEnding(),
  );
};

van.add(document.getElementById('reader-ui'), ReaderUI());
